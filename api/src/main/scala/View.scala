package psp
package std
package api

import ApiAliases._

trait View[+A] extends Any with Foreach[A] with RearSliceable[View[A]] {
  type   MapTo[+X] <: View[X]
  type SplitTo[+X] <: View.Split[X]

  def ++[A1 >: A](that: View[A1]): MapTo[A1]
  def zip[B](that: View[B]): MapTo[(A, B)]
  def calls: Int
  def collect[B](pf: A ?=> B): MapTo[B]
  def description: String
  def drop(n: Precise): MapTo[A]
  def dropIndex(index: Index): MapTo[A]
  def dropRight(n: Precise): MapTo[A]
  def dropWhile(p: Predicate[A]): MapTo[A]
  def filter(p: Predicate[A]): MapTo[A]
  def filterNot(p: Predicate[A]): MapTo[A]
  def flatMap[B](f: A => Foreach[B]): MapTo[B]
  def intersperse[A1 >: A](that: View[A1]): MapTo[A1]
  def map[B](f: A => B): MapTo[B]
  def partition(p: Predicate[A]): SplitTo[A]
  def sized(size: Precise): MapTo[A]
  def slice(range: IndexRange): MapTo[A]
  def span(p: Predicate[A]): SplitTo[A]
  def splitAt(index: Index): SplitTo[A]
  def take(n: Precise): MapTo[A]
  def takeRight(n: Precise): MapTo[A]
  def takeWhile(p: Predicate[A]): MapTo[A]
  def viewChain: Direct[View[_]]
  def viewRange: IndexRange
  def withFilter(p: Predicate[A]): MapTo[A]
}

trait FrontSliceable[+A] extends Any {
  def drop(n: Precise): A
  def take(n: Precise): A
  def slice(range: IndexRange): A
  def drop(n: Int): A = drop(Precise(n))
  def take(n: Int): A = take(Precise(n))
}

trait RearSliceable[+A] extends Any with FrontSliceable[A] {
  def dropRight(n: Precise): A
  def takeRight(n: Precise): A
  def dropRight(n: Int): A = dropRight(Precise(n))
  def takeRight(n: Int): A = takeRight(Precise(n))
}

object View {
  trait Atomic[+A] extends Any with View[A]
  trait Composite[A, +B] extends Any with View[B] { def prev: View[A] }
  trait Split[+A] extends Any {
    type Single[+A] <: View[A]
    def left: Single[A]
    def right: Single[A]
    def join: Single[A]
    def intersperse: Single[A]
  }
  trait Tuples[+A, +B] extends Any with View[(A, B)] {
    def lefts: View[A]
    def rights: View[B]
  }
}
