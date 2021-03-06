package psp
package tests

import std._, all._, StdShow._, Makes._

class StringViewTests {
  val ad: String    = ('a' to 'd').m.joinString
  val da: String    = ad.reverseChars
  val adda1: String = view(ad, da).join
  val adda2: String = view(ad, "c", da).join
  val adda3: String = view(ad, "pa", da).join

  def split(s: String)            = s splitAfter s.length / 2 mapRight (_.reverse)
  def isPalindrome(s: String)     = split(s).zip forall (_ === _)
  def isEvenPalindrome(s: String) = split(s) app (_ === _)

  @Test
  def stringTests(): Unit = {
    same(ad, ad.reverseChars.reverseChars)
    assert(isPalindrome(adda1), adda1)
    assert(isPalindrome(adda2), adda2)
    assert(!isEvenPalindrome(adda2), adda2)
    assert(!isPalindrome(adda3), adda3)
    sameDoc("[a].".r findAll adda3, "[ ab, ad ]")
    sameDoc("abcdefg" stripPrefix "a..", "abcdefg")
    sameDoc("abcdefg" stripPrefix "a..".r, "defg")
    same("123456" o (_ splitAfter 3.size mapEach (_.reverse) join), "321654")
    same("1234" o (_ reverse), "4321")
    same(Array(1, 2, 3) o (_ reverse), Array(3, 2, 1))
    same(scalaList(1, 2, 3) o (_ reverse), scalaList(3, 2, 1))
    same(javaList(1, 2, 3) o (_ reverse), javaList(3, 2, 1))
  }
}

class IntViewTests {
  type DView[A] = RView[A, Direct[A]]
  type DVInt    = DView[Int]
  type VIntInt  = View[Int -> Int]

  val ints: IntRange = 1 to 10
  val ints3: DVInt   = ints take 3
  val xints: DVInt   = view(304, 106, 25).as[Vec[Int]]
  val splt           = ints take 6 partition (_ % 2 === 0)
  val even3          = splt.leftView
  val odd3           = splt.rightView
  val zip3           = splt.zip
  val cro9           = splt.cross

  def reverseInt = ?[Order[Int]].flip

  @Test
  def collectionTests(): Unit = {
    same(scalaList(1, 1, 2, 2, 3) o (_.to[sciSet]) sorted, scalaList(1, 2, 3))
    sameDoc(scalaList(1, 2, 3), "List(1, 2, 3)")
    sameDoc(scala.Vector(1, 2, 3), "Vector(1, 2, 3)")
    sameDocAsToString(javaSet(1, 2, 3))
    sameDocAsToString(javaList(1, 2, 3))
    sameDocAsToString(javaMap(1 -> 2, 3 -> 4))
    sameDocAsToString(scalaSet(1, 2, 3))
    sameDocAsToString(scalaList(1, 2, 3))
    sameDocAsToString(scalaMap(1 -> 2, 3 -> 4))
    sameDoc(elems(1, 2, 3): Pset[Int], "{1, 2, 3}")
    sameDoc(1 :: 2 :: 3 :: Pnil(), "[ 1, 2, 3 ]")
    // sameDoc(pmap(1 -> 2, 3 -> 4), "{1: 2, 3: 4}")
  }

  @Test
  def orderedTests(): Unit = {
    def nint = none[Int]()

    junitAssert(
      view(1, 2) < view(1, 2, 3),
      view(1, 2) <= view(1, 2, 3),
      view(1, 2, 3) <= view(1, 2, 3),
      some(1) < some(2),
      some(2) <= some(2),
      nint < some(1),
      (nint -> 2) < (some(1) -> 0)
    )
    junitAssertFalse(
      view(1, 2) > view(1, 2, 3),
      view(1, 2) >= view(1, 2, 3),
      view(1, 2, 3) > view(1, 2, 3),
      some(1) >= some(2),
      some(2) > some(2),
      nint >= some(1),
      (nint -> 2) >= (some(1) -> 0)
    )
  }

  @Test
  def zipTests(): Unit = {
    sameDoc(ints take 3 zipTail, "[ 1 -> 2, 2 -> 3 ]")
    sameDoc((1 to 100000).zipTail drop 100 take 2, "[ 101 -> 102, 102 -> 103 ]")
    same(1 to 2 zip (4 to 5) map (_ + _), view(5, 7))
    same(zip3 map (_ - _), view(1, 1, 1))
    same(zip3 mapLeft (_ * 10) pairs, view(20 -> 1, 40 -> 3, 60 -> 5))
    same(zip3 mapRight (_ => 0) pairs, view(2 -> 0, 4  -> 0, 6  -> 0))
    same(splt.collate, view(2, 1, 4, 3, 6, 5))
    same(splt.join, view(2, 4, 6, 1, 3, 5))
    same(zip3 corresponds (_ > _), true)
    same(zipViews(ints3, ints3) corresponds (_ >= _), true)
    same(zipViews(ints3, ints3 :+ 8) corresponds (_ >= _), false)
  }

  @Test
  def noTypeClassNeededTests(): Unit = {
    same(1 nthTo 3, 0 indexUntil 3)
    same(1 to 3 map (_.nth), 0 to 2 map (_.index))
    same(5 +: ints3 :+ 5, view(5, 1, 2, 3, 5))
    same(cro9.force.size, Size(9))
    same(ints applyIndex Nth(2), 2)
    same(ints applyIndex _0, 1)
    same(ints count (_ < 3), 2)
    same(ints exists (_ < 10), true)
    same(ints exists (_ > 10), false)
    same(ints filter (_ > 5) head, 6)
    same(ints filterNot (_ > 5) last, 5)
    same(ints find (_ > 15) or 20, 20)
    same(ints find (_ > 5) or 20, 6)
    same(ints find (_ > 5), some(6))
    same(ints forall (_ < 10), false)
    same(ints forall (_ < 11), true)
    same(ints grep "^[47]$".r head, 4)
    same(ints grep "^[47]$".r last, 7)
    same(ints head, 1)
    same(ints indexWhere (_ < 1), Index(-12345)) // There's only one invalid index
    same(ints indexWhere (_ > 1), Index(1))
    same(ints indexWhere (_ > 1), Nth(2))
    same(ints last, 10)
    same(ints mapIf { case 1 => -1 } head, -1)
    same(ints max reverseInt, 1)
    same(ints reducel (_ + _), 55)
    same(ints reducer (_ + _), 55)
    same(ints sliceIndex Nth(2), view(2))
    same(ints sliceIndex Nth(20), view[Int]())
    same(ints sliceWhile (_ < 4, _ < 6), view(4, 5))
    same(ints sort reverseInt head, 10)
    same(ints takeToFirst (_ > 2), view(1, 2, 3))
    same(ints.init.last, 9)
    same(ints.max, 10)
    same(ints.span(_ < 4).collate, view(1, 4, 2, 5, 3, 6))
    same(ints.tail.head, 2)
    same(ints.toVec o (_ mapIf { case 1 => -1 }) size, Size(10))
    same(ints3 dropIndex Nth(2), view(1, 3))
    same(ints3 splitAround Nth(2) join, view(1, 3))
    same(ints3.foldl("x")((res, x) => pp"($res - $x)"), "(((x - 1) - 2) - 3)")
    same(ints3.foldl(0)(_ - _), -6)
    same(ints3.foldr("x")((x, res) => pp"($x - $res)"), "(1 - (2 - (3 - x)))")
    same(ints3.foldr(0)(_ - _), 2)
    same(xints sortWith (_ > _), view(304, 106, 25))
    same(xints.sort, view(25, 106, 304))
    same(xints.sortBy(_.any_s), view(106, 25, 304))
    same(xints.sortBy(_.any_s.reverseBytes.utf8String), view(304, 25, 106))
    same[View[Int]](ints drop 2 take 2, view(3, 4))
    same[View[Int]](ints slice (1 indexUntil 4), view(2, 3, 4))
    same[View[Int]](ints slice (3 nthTo 4), view(3, 4))
    same[View[Int]](ints.slice(2.index, 2.size), view(3, 4))
    same[View[Int]](ints.slice(Nth(2), Size(2)), view(2, 3))
  }

  @Test
  def twoDimensionalViewOps(): Unit = {
    same(ints3.inits, view(view(1, 2, 3), view(1, 2), view(1), view()))
    same(ints3.tails, view(view(1, 2, 3), view(2, 3), view(3), view()))
    same(ints3.tails.flatMap(x => x), view(1, 2, 3, 2, 3, 3))
  }

  @Test
  def emptyNeededTests(): Unit = {
    implicit def emptyInt = Empty[Int](0)

    val pf1: Int ?=> String = { case 5  => "bob" }
    val pf2: Int ?=> String = { case 50 => "bob" }

    same(ints zfirst pf1, "bob")
    same(ints zfirst pf2, "")
    same(ints zhead, 1)
    same(ints zlast, 10)
    same(ints zreducel (_ + _), 55)
    same(ints zreducer (_ + _), 55)
    same(ints3 zfoldl [Int] (_ - _), -6)
    same(ints3 zfoldr [Int] (_ - _), 2)
    same(view[Int]() zreducel (_ + _), 0)
  }

  @Test
  def readmeShowTests(): Unit = {
    val xs = 1 to 20 splitAfter 10.size
    val ys = zipCross(1 to 3, view("a", "bb"))
    val zs = ys eqBy (x => x, _.length)

    sameDoc(xs, "[ 1, 2, 3, ... ] / [ 11, 12, 13, ... ]")
    sameDoc(xs mapLeft (_ dropRight 8) join, "[ 1, 2, 11, ... ]")
    sameDoc(xs.zip filterRight (_ % 3 === 0), "[ 2 -> 12, 5 -> 15, 8 -> 18 ]")
    sameDoc(ys, "[ 1 -> a, 1 -> bb, 2 -> a, 2 -> bb, 3 -> a, 3 -> bb ]")
    sameDoc(zs, "[ 1 -> a, 2 -> bb ]")
    sameDoc(zs.rights joinWith '/', "a/bb")
  }
}
