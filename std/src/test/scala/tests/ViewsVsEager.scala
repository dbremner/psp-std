package psp
package tests

import psp.std._, all._, StdShow._
import org.scalacheck._, Prop.forAll, Gen._

class OperationCounts extends ScalacheckBundle {
  import Op._

  type LongOp = Op[Long, Long]

  private[this] var displaysRemaining = maxDisplay

  def bundle                 = "Operation Counts"
  def max: Long              = 100L
  def minSuccessful: Precise = 1000
  def maxDisplay: Precise    = 20

  def fullRange = 0L upTo max
  def lowHalf   = 0L upTo max / 2
  def highHalf  = max / 2 upTo max
  def genSmall  = 1L upTo max / 20
  def genRange  = (genSmall zipWith fullRange)(_ indexUntil _)

  private def divides(n: Long)  = ((_: Long) % n === 0) labeled pp"/$n"
  private def less(n: Long)     = ((_: Long) < n) labeled pp"<$n"
  private def multiply(n: Long) = ((_: Long) * n) labeled pp"*$n"
  private def pairup            = ((x: Long) => view(x, x)) labeled pp"=>(x,x)"

  def genOneOp: Gen[LongOp] = oneOf(
    lowHalf   ^^ (n => Drop[Long](n)),
    highHalf  ^^ (n => Take[Long](n)),
    fullRange ^^ (n => DropRight[Long](n)),
    fullRange ^^ (n => TakeRight[Long](n)),
    lowHalf   ^^ (n => DropWhile(less(n))),
    lowHalf   ^^ (n => TakeWhile(less(n))),
    genSmall  ^^ (n => Maps(multiply(n))),
    genSmall  ^^ (n => Filter(divides(n))),
    genSmall  ^^ (n => Collect(Fun.partial(divides(n), (_: Long) / n))),
    genSmall  ^^ (n => FlatMap(pairup)),
    genRange  ^^ (r => Slice[Long](r))
  )
  def genCompositeOp: Gen[LongOp] = genOneOp * (2 upTo 4) ^^ (_ reducel (_ ~ _))

  def composite: Gen[CompositeOp] = genCompositeOp ^^ CompositeOp

  val counter = OperableCounter(1L to max)

  case class CompositeOp(op: LongOp) {
    lazy val (maybeRes, views, eager) = counter(op ~ Take(3))
    lazy val countsPass               = views.accesses <= eager.accesses && views.allocations <= eager.allocations
    lazy val isPass                   = maybeRes.isRight && countsPass
    lazy val passed                   = sideEffect(isPass, maybeShow())

    def compare(lhs: Long, rhs: Long): Doc =
      cond(lhs <= rhs, "<=", ">") |> (op => fdoc"$lhs%3s $op%-2s $rhs%-3s")

    def counts1 = compare(views.accesses, eager.accesses)
    def counts2 = compare(views.allocations, eager.allocations)

    def res_s: Doc = maybeRes match {
      case scala.Right(res) => res
      case scala.Left(err)  => err
    }
    def pass_s = fp"|$op%-70s  $counts1%s  $counts2%s  // $res_s%s"
    def fail_s = pp"Inconsistent results for $op: $res_s"

    private def maybeShow(): Unit = {
      if (!isPass)
        println(fail_s)
      else if (isTestDebug || displaysRemaining > 0)
        sideEffect(println(pass_s), displaysRemaining -= 1)
    }
  }

  implicit def arbCompositeOp: Arbitrary[CompositeOp] = Arbitrary(composite)

  def compositeProp: Prop = forAll((_: CompositeOp).passed) minSuccessful minSuccessful
  def props() = vec[NamedProp](
    pp"Showing $maxDisplay/$minSuccessful ops, compares accesses/allocations views v. eager" -> Prop(true),
    pp"views never performs more accesses or allocations than eager" -> compositeProp
  )
}

object OperableCounter {
  import Operable._
  import Op._

  // show-related accesses not to be counted
  implicit def showCountXs[A: Show] : Show[CountXs[A]] = Show.by(_.xs)

  class CountXs[A](val xs: Direct[A], val counter: OpCount) extends Direct[A] {
    def size: Precise       = xs.size
    def apply(idx: Vdex): A = sideEffect(xs(idx), counter access idx)
  }

  case class ViewOpCount(label: String, accesses: Long, allocations: Long)

  class OpCount(label: String) {
    private var _access, _alloc    = 0L

    def reset(): Unit              = sideEffect(_access = 0, _alloc = 0)
    def access(vdex: Vdex): Unit   = _access += 1
    def alloc(size: Precise): Unit = _alloc += size.getLong
    def result(): ViewOpCount      = ViewOpCount(label, _access, _alloc)
  }

  implicit object OperableCountXs extends Operable[CountXs] {
    /** This is the key moment. We decompose the composite
     *  operation and force it at each step so as to obtain access
     *  and allocation counts. If we simply pass it through, only
     *  one operation will be applied, leaving the eager case about
     *  the same as the view case.
     */
    def apply[A, B](xs: CountXs[A])(op: Op[A, B]): CountXs[B] = op match {
      case Compose(o1, o2) => apply(apply(xs)(o1))(o2)
      case _               => OperableView(xs)(op).force |> (res => sideEffect(new CountXs(res, xs.counter), xs.counter alloc res.size))
    }
  }

  class Compared(range: LongRange) {
    def apply(op: Op[Long, Long]) = {
      val c1 = new OpCount("views")
      val c2 = new OpCount("eager")
      val r1 = op[View](new CountXs(range, c1)).force
      val r2 = op[CountXs](new CountXs(range, c2)).force

      val res = r1 === r2 match {
        case true  => scala.Right(r1)
        case false => scala.Left(pp"$r1 != $r2")
      }
      ((res, c1.result, c2.result))
    }
  }

  def apply(range: LongRange) = new Compared(range)
}