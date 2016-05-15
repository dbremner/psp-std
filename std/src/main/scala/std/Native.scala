package psp
package std

import api._, all._

/** "Native" psp collections.
  */
sealed abstract class Plist[A] extends Each[A] {
  def head: A
  def tail: Plist[A]
  def ::(head: A): Plist[A] = Pcons(head, this)
  @inline final def foreach(f: A => Unit): Unit = {
    def loop(xs: Plist[A]): Unit = xs match {
      case Pcons(hd, tl) => f(hd); loop(tl)
      case _             =>
    }
    loop(this)
  }
}
final case class Pcons[A](head: A, tail: Plist[A]) extends Plist[A] {
  def size = Size.NonEmpty
}
final case object Pnil extends Plist[Nothing] {
  def size = Size(0)
  def head = abort("Pnil.head")
  def tail = abort("Pnil.tail")
}
final class Pset[A](private val xs: sciSet[A]) extends ExSet[A] {
  def basis                 = xs.m
  def equiv                 = byEquals
  def apply(x: A): Bool     = xs(x)
  def foreach(f: A => Unit) = xs foreach f
  def size: Precise         = xs.size

  def map[B](f: A => B): ExMap[A, B]               = Fun.finite(this, f)
  def flatMap[B](f: A => ExMap[A, B]): ExMap[A, B] = Fun.finite(this, x => f(x)(x))
}
final class Vec[A](private val underlying: sciVector[A]) extends AnyVal with Direct[A] {
  private def make(f: sciVector[A] => sciVector[A]): Vec[A] = new Vec[A](f(underlying))

  def isEmpty           = length <= 0
  def length: Int       = underlying.length
  def size: Precise     = Size(length)
  def apply(i: Vdex): A = underlying(i.getInt)

  def updated(i: Vdex, elem: A): Vec[A] = make(_.updated(i.getInt, elem))
  def :+(elem: A): Vec[A]               = make(_ :+ elem)
  def +:(elem: A): Vec[A]               = make(elem +: _)
  def ++(that: Vec[A]): Vec[A]          = cond(that.isEmpty, this, cond(this.isEmpty, that, make(_ ++ that.trav)))

  def drop(n: Vdex): Vec[A]      = make(_ drop n.getInt)
  def dropRight(n: Vdex): Vec[A] = make(_ dropRight n.getInt)
  def take(n: Vdex): Vec[A]      = make(_ take n.getInt)
  def takeRight(n: Vdex): Vec[A] = make(_ takeRight n.getInt)

  @inline def foreach(f: A => Unit): Unit =
    ll.foreachInt(0, length - 1, i => f(underlying(i)))
}
