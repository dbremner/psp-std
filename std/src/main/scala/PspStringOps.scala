package psp
package std

import api._
import java.{ lang => jl }

/** Rather than struggle with ambiguities with Predef.augmentString, we'll
 *  bury it and reimplement what we want.
 */
final class PspStringOps(val self: String) extends AnyVal with ForceShowDirect {
  def r: Regex     = Regex(self)
  def u: jUrl      = jUrl(self)
  def s: Doc       = Doc.Literal(self)
  def to_s: String = self

  def * (n: Int): String     = this * n.size
  def * (n: Precise): String = n timesConst self mkString ""

  def append(that: String): String                  = self + that
  def bytes: Array[Byte]                            = self.getBytes
  def capitalize: String                            = ifNonEmpty("" + self.head.toUpper + self.tail)
  def chars: Array[Char]                            = self.toCharArray
  def containsChar(ch: Char): Boolean               = chars.m contains ch
  def dollarSegments: Vec[String]                   = splitChar('$')
  def dottedSegments: Vec[String]                   = splitChar('.')
  def format(args : Any*): String                   = java.lang.String.format(self, args map unwrapArg: _*)
  def ifNonEmpty(body: => String): String           = if (isEmpty) "" else body
  def isAllWhitespace                               = self matches """[\s]*"""
  def isNonEmptyDigits                              = self matches """^[\d]+$"""
  def length                                        = self.length
  def lineVector: Vec[String]                       = splitChar('\n')
  def mapChars(pf: Char ?=> Char): String           = self map (c => if (pf isDefinedAt c) pf(c) else c) build
  def mapLines(f: ToSelf[String]): String           = mapSplit('\n')(f)
  def mapSplit(ch: Char)(f: ToSelf[String]): String = splitChar(ch) map f mkString ch.toString
  def nonEmpty: Boolean                             = onull.length > 0
  def processEscapes: String                        = scala.StringContext processEscapes self
  def remove(regex: Regex): String                  = regex matcher self replaceFirst ""
  def replaceChar(pair: Char -> Char): String       = self.replace(pair._1, pair._2)
  def reverse: String                               = new String(chars.inPlace.reverse)
  def sanitize: String                              = mapChars { case x if x.isControl => '?' }
  def size: IntSize                                 = Precise(self.length)
  def splitChar(ch: Char): Vec[String]              = splitRegex(Regex quote ch.any_s)
  def splitRegex(r: Regex): Vec[String]             = r.pattern split self toVec
  def stripMargin(marginChar: Char): String         = mapLines(_ remove ("""^\s*[""" + marginChar + "]").r)
  def stripMargin: String                           = stripMargin('|')
  def stripPrefix(prefix: String): String           = foldPrefix(prefix)(self, s => s)
  def stripSuffix(suffix: String): String           = foldSuffix(suffix)(self, s => s)
  def toBigInt: BigInt                              = scala.math.BigInt(self)
  def toDecimal: BigDecimal                         = scala.math.BigDecimal(self)
  def toDouble: Double                              = jl.Double parseDouble dropSuffix(self, "dD")
  def toFloat: Float                                = jl.Float parseFloat dropSuffix(self, "fF")
  def toInt: Int                                    = foldPrefix("0x")(jl.Integer parseInt self, s => jl.Integer.parseInt(s, 16))
  def toLong: Long                                  = foldPrefix("0x")(jl.Long parseLong dropSuffix(self, "lL"), s => jl.Long.parseLong(dropSuffix(s, "lL"), 16))
  def trimLines: String                             = mapLines(_.trim).trim

  private def isEmpty = onull == ""
  private def onull   = if (self eq null) "" else self

  private def unwrapArg(arg: Any): AnyRef = arg.matchOr(arg.toRef) { case x: ScalaNumber => x.underlying }
  private def foldPrefix[A](prefix: String)(none: => A, some: String => A): A = foldRemove(prefix.r.literal.starts)(none, some)
  private def foldRemove[A](r: Regex)(none: => A, some: String => A): A       = remove(r) match { case `self` => none ; case s => some(s) }
  private def foldSuffix[A](suffix: String)(none: => A, some: String => A): A = foldRemove(suffix.r.literal.ends)(none, some)
  private def dropSuffix(s: String, drop: String)                             = s remove drop.r.characterClass.ends
}
