package kotlin

import java.io.StringReader

inline fun String.lastIndexOf(str: String)  = (this as java.lang.String).lastIndexOf(str)

inline fun String.lastIndexOf(ch: Char) = (this as java.lang.String).lastIndexOf(ch.toString())

inline fun String.equalsIgnoreCase(anotherString: String) = (this as java.lang.String).equalsIgnoreCase(anotherString)

inline fun String.indexOf(str : String) = (this as java.lang.String).indexOf(str)

inline fun String.indexOf(str : String, fromIndex : Int) = (this as java.lang.String).indexOf(str, fromIndex)

inline fun String.replace(oldChar: Char, newChar : Char) = (this as java.lang.String).replace(oldChar, newChar).sure()

inline fun String.replaceAll(regex: String, replacement : String) = (this as java.lang.String).replaceAll(regex, replacement).sure()

inline fun String.trim() = (this as java.lang.String).trim().sure()

/** Returns the string with leading and trailing text matching the given string removed */
inline fun String.trim(text: String) = trimLeading(text).trimTrailing(text)

/** Returns the string with the prefix and postfix text trimmed */
inline fun String.trim(prefix: String, postfix: String) = trimLeading(prefix).trimTrailing(postfix)

/** Returns the string with the leading prefix of this string removed */
inline fun String.trimLeading(prefix: String): String {
    var answer = this
    if (answer.startsWith(prefix)) {
        answer = answer.substring(prefix.length())
    }
    return answer
}

/** Returns the string with the trailing postfix of this string removed */
inline fun String.trimTrailing(postfix: String): String {
    var answer = this
    if (answer.endsWith(postfix)) {
        answer = answer.substring(0, length() - postfix.length())
    }
    return answer
}

inline fun String.toUpperCase() = (this as java.lang.String).toUpperCase().sure()

inline fun String.toLowerCase() = (this as java.lang.String).toLowerCase().sure()

inline fun String.length() = (this as java.lang.String).length()

inline fun String.getBytes() = (this as java.lang.String).getBytes().sure()

inline fun String.toCharArray() = (this as java.lang.String).toCharArray().sure()

inline fun String.format(format : String, vararg args : Any?)  = java.lang.String.format(format, args).sure()

inline fun String.split(regex : String)  = (this as java.lang.String).split(regex)

inline fun String.substring(beginIndex : Int) = (this as java.lang.String).substring(beginIndex).sure()

inline fun String.substring(beginIndex : Int, endIndex : Int)  = (this as java.lang.String).substring(beginIndex, endIndex).sure()

inline fun String.startsWith(prefix: String) = (this as java.lang.String).startsWith(prefix)

inline fun String.startsWith(prefix: String, toffset: Int) = (this as java.lang.String).startsWith(prefix, toffset)

inline fun String.contains(seq: CharSequence) : Boolean = (this as java.lang.String).contains(seq)

inline fun String.endsWith(suffix: String) : Boolean = (this as java.lang.String).endsWith(suffix)

inline val String.size : Int
get() = length()

inline val String.reader : StringReader
get() = StringReader(this)

// "constructors" for String

inline fun String(bytes : ByteArray, offset : Int, length : Int, charsetName : String) = java.lang.String(bytes, offset, length, charsetName) as String

inline fun String(bytes : ByteArray, offset : Int, length : Int, charset : java.nio.charset.Charset) = java.lang.String(bytes, offset, length, charset) as String

inline fun String(bytes : ByteArray, charsetName : String?) = java.lang.String(bytes, charsetName) as String

inline fun String(bytes : ByteArray, charset : java.nio.charset.Charset) = java.lang.String(bytes, charset) as String

inline fun String(bytes : ByteArray, i : Int, i1 : Int) = java.lang.String(bytes, i, i1) as String

inline fun String(bytes : ByteArray) = java.lang.String(bytes) as String

inline fun String(chars : CharArray) = java.lang.String(chars) as String

inline fun String(stringBuffer : java.lang.StringBuffer) = java.lang.String(stringBuffer) as String

inline fun String(stringBuilder : java.lang.StringBuilder) = java.lang.String(stringBuilder) as String

/** Returns true if the string is not null and not empty */
inline fun String?.notEmpty() : Boolean = this != null && this.length() > 0

inline fun String.toByteArray(encoding: String?=null):ByteArray {
    if(encoding==null) {
        return (this as java.lang.String).getBytes().sure()
    } else {
        return (this as java.lang.String).getBytes(encoding).sure()
    }
}
inline fun String.toByteArray(encoding: java.nio.charset.Charset):ByteArray =  (this as java.lang.String).getBytes(encoding).sure()

inline fun String.toBoolean() = java.lang.Boolean.parseBoolean(this).sure()
inline fun String.toShort() = java.lang.Short.parseShort(this).sure()
inline fun String.toInt() = java.lang.Integer.parseInt(this).sure()
inline fun String.toLong() = java.lang.Long.parseLong(this).sure()
inline fun String.toFloat() = java.lang.Float.parseFloat(this).sure()
inline fun String.toDouble() = java.lang.Double.parseDouble(this).sure()

/**
 * Converts the string into a regular expression [[Pattern]] optionally
 * with the specified flags from [[Pattern]] or'd together
 * so that strings can be split or matched on.
 */
inline fun String.toRegex(flags: Int=0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags).sure()
}

/**
Iterator for characters of given CharSequence
*/
inline fun CharSequence.iterator() : CharIterator = object: jet.CharIterator() {
   private var index = 0

   public override fun nextChar(): Char = get(index++)

   public override val hasNext: Boolean
   get() = index < length
}

inline fun String.replaceFirst(regex : String, replacement : String) = (this as java.lang.String).replaceFirst(regex, replacement).sure()

inline fun String.charAt(index : Int) = (this as java.lang.String).charAt(index).sure()

inline fun String.split(regex : String, limit : Int) = (this as java.lang.String).split(regex, limit).sure()

inline fun String.codePointAt(index : Int) = (this as java.lang.String).codePointAt(index).sure()

inline fun String.codePointBefore(index : Int) = (this as java.lang.String).codePointBefore(index).sure()

inline fun String.codePointCount(beginIndex : Int, endIndex : Int) = (this as java.lang.String).codePointCount(beginIndex, endIndex)

inline fun String.compareToIgnoreCase(str : String) = (this as java.lang.String).compareToIgnoreCase(str).sure()

inline fun String.concat(str : String) = (this as java.lang.String).concat(str).sure()

inline fun String.contentEquals(cs : CharSequence) = (this as java.lang.String).contentEquals(cs).sure()

inline fun String.contentEquals(sb : StringBuffer) = (this as java.lang.String).contentEquals(sb).sure()

inline fun String.getBytes(charset : java.nio.charset.Charset) = (this as java.lang.String).getBytes(charset).sure()

inline fun String.getBytes(charsetName : String) = (this as java.lang.String).getBytes(charsetName).sure()

inline fun String.getChars(srcBegin : Int, srcEnd : Int, dst : CharArray, dstBegin : Int) = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin).sure()

inline fun String.indexOf(ch : Char) = (this as java.lang.String).indexOf(ch.toString()).sure()

inline fun String.indexOf(ch : Char, fromIndex : Int) = (this as java.lang.String).indexOf(ch.toString(), fromIndex).sure()

inline fun String.intern() = (this as java.lang.String).intern().sure()

inline fun String.isEmpty() = (this as java.lang.String).isEmpty().sure()

inline fun String.lastIndexOf(ch : Char, fromIndex : Int) = (this as java.lang.String).lastIndexOf(ch.toString(), fromIndex).sure()

inline fun String.lastIndexOf(str : String, fromIndex : Int) = (this as java.lang.String).lastIndexOf(str, fromIndex).sure()

inline fun String.matches(regex : String) = (this as java.lang.String).matches(regex).sure()

inline fun String.offsetByCodePoints(index : Int, codePointOffset : Int) = (this as java.lang.String).offsetByCodePoints(index, codePointOffset).sure()

inline fun String.regionMatches(ignoreCase : Boolean, toffset : Int, other : String, ooffset : Int, len : Int) = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len).sure()

inline fun String.regionMatches(toffset : Int, other : String, ooffset : Int, len : Int) = (this as java.lang.String).regionMatches(toffset, other, ooffset, len).sure()

inline fun String.replace(target : CharSequence, replacement : CharSequence) = (this as java.lang.String).replace(target, replacement).sure()

inline fun String.subSequence(beginIndex : Int, endIndex : Int) = (this as java.lang.String).subSequence(beginIndex, endIndex).sure()

inline fun String.toLowerCase(locale : java.util.Locale) = (this as java.lang.String).toLowerCase(locale).sure()

inline fun String.toUpperCase(locale : java.util.Locale) = (this as java.lang.String).toUpperCase(locale).sure()

/** Returns the string if it is not null or the empty string if its null */
inline fun String?.orEmpty(): String = this ?: ""


// "Extension functions" for CharSequence

inline fun CharSequence.length() = (this as java.lang.CharSequence).length()

inline val CharSequence.size : Int
get() = length()

inline fun CharSequence.charAt(index : Int) = (this as java.lang.CharSequence).charAt(index)

inline fun CharSequence.get(index : Int) = charAt(index)

inline fun CharSequence.subSequence(start : Int, end : Int) = (this as java.lang.CharSequence).subSequence(start, end)

inline fun CharSequence.get(start : Int, end : Int) = subSequence(start, end)

inline fun CharSequence.toString() = (this as java.lang.CharSequence).toString()