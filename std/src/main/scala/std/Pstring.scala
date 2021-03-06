package psp
package std

import all._, StdShow._
import java.{ lang => jl }
import java.util.regex.{ Matcher, Pattern }
import jl.Integer.parseInt, jl.Long.parseLong
import Regex.WS
import PChar._

object PChar {
  val toUpper: Char => Char   = jl.Character toUpperCase _
  val isControl: Char => Bool = jl.Character isISOControl _
}

final class Pstring(val self: String) extends AnyVal with ShowSelf {
  import self.{ toCharArray => chars }

  def *(n: Precise): String                         = Makes const self take n joinString
  def append(that: String): String                  = self + that /** Note to self: don't touch this `+`. */
  def bytes: Array[Byte]                            = self.getBytes
  def capitalize: String                            = self o (_ splitAfter _1 mapLeft (_ map toUpper) join)
  def charSeq: scSeq[Char]                          = chars.m.seq
  def collect(pf: Char ?=> Char): String            = chars collect pf force
  def containsChar(ch: Char): Boolean               = chars.m contains ch
  def format(args: Any*): String                    = stringFormat(self, args: _*)
  def lines: View[String]                           = splitChar('\n')
  def lit: Doc                                      = Doc(self)
  def mapChars(pf: Char ?=> Char): String           = collect(pf orElse { case x => x })
  def mapLines(f: ToSelf[String]): String           = mapSplit('\n')(f)
  def mapWords(f: ToSelf[String]): String           = mapSplit(' ')(f)
  def mapSplit(ch: Char)(f: ToSelf[String]): String = splitChar(ch) map f joinWith ch
  def prepend(that: String): String                 = that + self
  def r: Regex                                      = Regex(self)
  def removeAll(regex: Regex): String               = regex matcher self replaceAll ""
  def removeFirst(regex: Regex): String             = regex matcher self replaceFirst ""
  def reverseBytes: Array[Byte]                     = bytes.inPlace.reverse
  def reverseChars: String                          = chars.inPlace.reverse.utf8String
  def sanitize: String                              = this mapChars (ScalaFun const '?' filter isControl)
  def splitChar(ch: Char): View[String]             = splitRegex(Regex quote ch.any_s)
  def splitRegex(r: Regex): View[String]            = View(r.pattern split self)
  def stripMargin(ch: Char): String                 = mapLines(_ stripPrefix WS <> ch.r)
  def stripMargin: String                           = stripMargin('|')
  def stripPrefix(prefix: String): String           = stripPrefix(prefix.r.literal)
  def stripPrefix(r: Regex): String                 = removeAll(r.starts)
  def stripSuffix(r: Regex): String                 = removeAll(r.ends)
  def stripSuffix(suffix: String): String           = stripSuffix(suffix.r.literal)
  def surround(x: PairOf[String]): String           = surround(fst(x), snd(x))
  def surround(s: String, e: String): String        = s append self append e
  def to_s: String                                  = self

  def ifPrefix[A](prefix: String)(f: String => A): Opt[A] =
    zcond(self startsWith prefix, some(f(self substring prefix.length)))

  def toBigInt: BigInt = scala.math.BigInt(self)
  def toInt: Int       = ifPrefix("0x")(parseInt(_, 16)) or parseInt(self)
  def toLong: Long     = ifPrefix("0x")(parseLong(_, 16)) or parseLong(self)

  def <>(rhs: Doc): Doc  = lit ~ rhs
  def <+>(rhs: Doc): Doc = lit ~ Doc.SP ~ rhs
}

final class Regex(val pattern: Pattern) extends ShowSelf {
  def matcher(input: jCharSequence): Matcher = pattern matcher input

  def findAll(s: String): Direct[String] = {
    def loop(m: Matcher): View[String] = if (m.find) m.group +: loop(m) else view()
    loop(matcher(s)).toVec
  }

  def append(e: String): Regex               = mapRegex(_ + e)
  def ends: Regex                            = cond(to_s endsWith "$", this, append("$"))
  def flags: Int                             = pattern.flags
  def isMatch(input: jCharSequence): Boolean = matcher(input).matches
  def isMatch[A: Show](x: A): Boolean        = isMatch(x.pp)
  def literal: Regex                         = surround("\\Q", "\\E") // Not setFlag(LITERAL) lest further regex additions be misinterpreted
  def mapRegex(f: ToSelf[String]): Regex     = Regex(f(to_s), flags)
  def prepend(s: String): Regex              = mapRegex(s + _)
  def starts: Regex                          = cond(to_s startsWith "^", this, prepend("^"))
  def surround(s: String, e: String): Regex  = mapRegex(_.surround(s, e))
  def to_s: String                           = pp"$pattern"

  def <>(that: Regex): Regex = mapRegex(_ + that.pattern)
  def |(that: Regex): Regex  = mapRegex(_ + "|" + that.pattern)
}

object Regex extends (String => Regex) {
  val WS = apply("\\s*")

  def apply(ch: Char): Regex              = apply(pp"[$ch]")
  def quote(s: String): Regex             = apply(Pattern quote s)
  def apply(s: String): Regex             = new Regex(Pattern compile s)
  def apply(s: String, flags: Int): Regex = new Regex(Pattern.compile(s, flags))
}
