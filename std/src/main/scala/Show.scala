package psp
package std

import api._
import StdShow._

class FullRenderer extends Renderer {
  def minElements: Precise = 3.size
  def maxElements: Precise = 10.size

  def showEach(xs: Each[Doc]): String = "[ " + (xs splitAt maxElements.lastIndex match {
    case Split(xs, ys) if ys.isEmpty => xs map show mk_s ", "
    case Split(xs, _)                => (xs take minElements map show mk_s ", ") + ", ..."
  }) + " ]"

  def show(x: Doc): String = x match {
    case Doc.NoDoc           => ""
    case Doc.Cat(l, r)       => show(l) + show(r)
    case Doc.Group(xs)       => showEach(xs)
    case Doc.Shown(value, z) => z show value
    case Doc.Literal(s)      => s
  }
}

final class ShowInterpolator(val stringContext: StringContext) extends AnyVal {
  def escapedParts    = stringContext.parts map (_.processEscapes)
  def escaped: String = escapedParts mk_s ""

  /** The type of args forces all the interpolation variables to
   *  be of a type which is implicitly convertible to Doc.
   */
  def show(args: Doc*): String = StringContext(escapedParts: _*).raw(args.map(_.render): _*)
  def pp(args: Doc*): String   = show(args: _*)

  /** Can't see any way to reuse the standard (type-safe) f-interpolator, will
   *  apparently have to reimplement it entirely.
   */
  def fshow(args: Doc*): String = escaped.format(args.map(_.render): _*)

  final def sm(args: Any*): String = {
    def isLineBreak(c: Char) = c == '\n' || c == '\f' // compatible with StringLike#isLineBreak
    def stripTrailingPart(s: String): String = {
      val index        = s indexWhere isLineBreak
      val pre: String  = s take index.sizeExcluding force
      val post: String = s drop index.sizeExcluding force;
      pre append post.stripMargin
    }
    val stripped: sciList[String] = stringContext.parts.toList match {
      case head :: tail => head.stripMargin :: (tail map stripTrailingPart)
      case Nil          => Nil
    }
    (new StringContext(stripped: _*).raw(args: _*)).trim
  }
}

object Show {
  /** This of course is not implicit as that would defeat the purpose of the endeavor.
   *  There is however an implicit universal instance in the Unsafe object.
   */
  val Inherited: Show[Any] = apply[Any] {
    case null          => ""
    case x: ShowDirect => x.to_s
    case x             => x.toString
  }

  def apply[A](f: ToString[A]): Show[A] = new Impl[A](f)

  final class Impl[-A](val f: ToString[A]) extends AnyVal with Show[A] { def show(x: A) = f(x) }
}

/** An incomplete selection of show compositors.
 *  Not printing the way scala does.
 */
trait ShowInstances extends ShowEach {
  implicit def showBoolean: Show[Boolean]     = inheritShow
  implicit def showChar: Show[Char]           = inheritShow
  implicit def showDouble: Show[Double]       = inheritShow
  implicit def showInt: Show[Int]             = inheritShow
  implicit def showLong: Show[Long]           = inheritShow
  implicit def showPath: Show[Path]           = inheritShow
  implicit def showString: Show[String]       = inheritShow
  implicit def showThrowable: Show[Throwable] = inheritShow

  implicit def showClass: Show[jClass]                                  = Show(_.shortName)
  implicit def showDirect: Show[ShowDirect]                             = Show(_.to_s)
  implicit def showIndex: Show[Index]                                   = showBy(_.get)
  implicit def showOption[A: Show] : Show[Option[A]]                    = Show(_.fold("-")(_.render))
  implicit def showPair[A: Show, B: Show] : Show[A -> B]                = Show(x => x._1 ~ " -> " ~ x._2 render)
  implicit def showStackTraceElement: Show[java.lang.StackTraceElement] = Show(x => "\tat$x\n")

  implicit def showSize: Show[Size] = Show[Size] {
    case Precise(size)         => show"$size"
    case Bounded(lo, Infinite) => show"$lo+"
    case Bounded(lo, hi)       => show"[$lo,$hi]"
    case Infinite              => "<inf>"
  }
}

trait ShowEach0 {
  implicit def showView[A: Show](implicit z: FullRenderer): Show[View[A]] = showBy[View[A]](z showEach _.toEach.map(_.doc))
}
trait ShowEach1 extends ShowEach0 {
  implicit def showEach[A: Show](implicit z: FullRenderer): Show[Each[A]] = Show(xs => z showEach (xs map (_.doc)))
}
trait ShowEach extends ShowEach1 {
  implicit def showExMap[K: Show, V: Show] : Show[ExMap[K, V]]        = Show(xs => tabular(xs.entries.pairs)(_.render))
  implicit def showZipped[A1: Show, A2: Show] : Show[ZipView[A1, A2]] = showBy[ZipView[A1, A2]](_.pairs)
  implicit def showArray[A: Show] : Show[Array[A]]                    = showBy[Array[A]](Direct.fromArray)
  implicit def showJavaEnum[A <: jEnum[A]] : Show[jEnum[A]]           = inheritShow
}
