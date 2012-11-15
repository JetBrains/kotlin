package kotlin

import java.io.StringReader

public inline fun String.lastIndexOf(str: String) : Int = (this as java.lang.String).lastIndexOf(str)

public inline fun String.lastIndexOf(ch: Char) : Int = (this as java.lang.String).lastIndexOf(ch.toString())

public inline fun String.equalsIgnoreCase(anotherString: String) : Boolean = (this as java.lang.String).equalsIgnoreCase(anotherString)

public inline fun String.hashCode() : Int = (this as java.lang.String).hashCode()

public inline fun String.indexOf(str : String) : Int = (this as java.lang.String).indexOf(str)

public inline fun String.indexOf(str : String, fromIndex : Int) : Int = (this as java.lang.String).indexOf(str, fromIndex)

public inline fun String.replace(oldChar: Char, newChar : Char) : String = (this as java.lang.String).replace(oldChar, newChar)

public inline fun String.replaceAll(regex: String, replacement : String) : String = (this as java.lang.String).replaceAll(regex, replacement)

public inline fun String.trim() : String = (this as java.lang.String).trim()

public inline fun String.toUpperCase() : String = (this as java.lang.String).toUpperCase()

public inline fun String.toLowerCase() : String = (this as java.lang.String).toLowerCase()

public inline fun String.length() : Int = (this as java.lang.String).length()

public inline fun String.getBytes() : ByteArray = (this as java.lang.String).getBytes()

public inline fun String.toCharArray() : CharArray = (this as java.lang.String).toCharArray()

public inline fun String.toCharList(): List<Char> = toCharArray().toList()

public inline fun String.format(format : String, vararg args : Any?) : String = java.lang.String.format(format, args)

public inline fun String.split(regex : String) : Array<out String> = (this as java.lang.String).split(regex)

public inline fun String.split(ch : Char) : Array<out String> = (this as java.lang.String).split(java.util.regex.Pattern.quote(ch.toString()))

public inline fun String.substring(beginIndex : Int) : String = (this as java.lang.String).substring(beginIndex)

public inline fun String.substring(beginIndex : Int, endIndex : Int) : String = (this as java.lang.String).substring(beginIndex, endIndex)

public inline fun String.startsWith(prefix: String) : Boolean = (this as java.lang.String).startsWith(prefix)

public inline fun String.startsWith(prefix: String, toffset: Int) : Boolean = (this as java.lang.String).startsWith(prefix, toffset)

public inline fun String.startsWith(ch: Char) : Boolean = (this as java.lang.String).startsWith(ch.toString())

public inline fun String.contains(seq: CharSequence) : Boolean = (this as java.lang.String).contains(seq)

public inline fun String.endsWith(suffix: String) : Boolean = (this as java.lang.String).endsWith(suffix)

public inline fun String.endsWith(ch: Char) : Boolean = (this as java.lang.String).endsWith(ch.toString())

// "constructors" for String

public inline fun String(bytes : ByteArray, offset : Int, length : Int, charsetName : String) : String = java.lang.String(bytes, offset, length, charsetName) as String

public inline fun String(bytes : ByteArray, offset : Int, length : Int, charset : java.nio.charset.Charset) : String = java.lang.String(bytes, offset, length, charset) as String

public inline fun String(bytes : ByteArray, charsetName : String) : String = java.lang.String(bytes, charsetName) as String

public inline fun String(bytes : ByteArray, charset : java.nio.charset.Charset) : String = java.lang.String(bytes, charset) as String

public inline fun String(bytes : ByteArray, i : Int, i1 : Int) : String = java.lang.String(bytes, i, i1) as String

public inline fun String(bytes : ByteArray) : String = java.lang.String(bytes) as String

public inline fun String(chars : CharArray) : String = java.lang.String(chars) as String

public inline fun String(stringBuffer : java.lang.StringBuffer) : String = java.lang.String(stringBuffer) as String

public inline fun String(stringBuilder : java.lang.StringBuilder) : String = java.lang.String(stringBuilder) as String

public inline fun String.replaceFirst(regex : String, replacement : String) : String = (this as java.lang.String).replaceFirst(regex, replacement)

public inline fun String.charAt(index : Int) : Char = (this as java.lang.String).charAt(index)

public inline fun String.split(regex : String, limit : Int) : Array<out String> = (this as java.lang.String).split(regex, limit)

public inline fun String.codePointAt(index : Int) : Int = (this as java.lang.String).codePointAt(index)

public inline fun String.codePointBefore(index : Int) : Int = (this as java.lang.String).codePointBefore(index)

public inline fun String.codePointCount(beginIndex : Int, endIndex : Int) : Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

public inline fun String.compareToIgnoreCase(str : String) : Int = (this as java.lang.String).compareToIgnoreCase(str)

public inline fun String.concat(str : String) : String = (this as java.lang.String).concat(str)

public inline fun String.contentEquals(cs : CharSequence) : Boolean = (this as java.lang.String).contentEquals(cs)

public inline fun String.contentEquals(sb : StringBuffer) : Boolean = (this as java.lang.String).contentEquals(sb)

public inline fun String.getBytes(charset : java.nio.charset.Charset) : ByteArray = (this as java.lang.String).getBytes(charset)

public inline fun String.getBytes(charsetName : String) : ByteArray = (this as java.lang.String).getBytes(charsetName)

public inline fun String.getChars(srcBegin : Int, srcEnd : Int, dst : CharArray, dstBegin : Int) : Tuple0 = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin)

public inline fun String.indexOf(ch : Char) : Int = (this as java.lang.String).indexOf(ch.toString())

public inline fun String.indexOf(ch : Char, fromIndex : Int) : Int = (this as java.lang.String).indexOf(ch.toString(), fromIndex)

public inline fun String.intern() : String = (this as java.lang.String).intern()

public inline fun String.isEmpty() : Boolean = (this as java.lang.String).isEmpty()

public inline fun String.lastIndexOf(ch : Char, fromIndex : Int) : Int = (this as java.lang.String).lastIndexOf(ch.toString(), fromIndex)

public inline fun String.lastIndexOf(str : String, fromIndex : Int) : Int = (this as java.lang.String).lastIndexOf(str, fromIndex)

public inline fun String.matches(regex : String) : Boolean = (this as java.lang.String).matches(regex)

public inline fun String.offsetByCodePoints(index : Int, codePointOffset : Int) : Int = (this as java.lang.String).offsetByCodePoints(index, codePointOffset)

public inline fun String.regionMatches(ignoreCase : Boolean, toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len)

public inline fun String.regionMatches(toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(toffset, other, ooffset, len)

public inline fun String.replace(target : CharSequence, replacement : CharSequence) : String = (this as java.lang.String).replace(target, replacement)

public inline fun String.subSequence(beginIndex : Int, endIndex : Int) : CharSequence = (this as java.lang.String).subSequence(beginIndex, endIndex)

public inline fun String.toLowerCase(locale : java.util.Locale) : String = (this as java.lang.String).toLowerCase(locale)

public inline fun String.toUpperCase(locale : java.util.Locale) : String = (this as java.lang.String).toUpperCase(locale)


public inline fun CharSequence.charAt(index : Int) : Char = (this as java.lang.CharSequence).charAt(index)

public inline fun CharSequence.get(index : Int) : Char = charAt(index)

public inline fun CharSequence.subSequence(start : Int, end : Int) : CharSequence? = (this as java.lang.CharSequence).subSequence(start, end)

public inline fun CharSequence.get(start : Int, end : Int) : CharSequence? = subSequence(start, end)

public inline fun CharSequence.toString() : String? = (this as java.lang.CharSequence).toString()

public inline fun CharSequence.length() : Int = (this as java.lang.CharSequence).length()


public inline fun String.toByteArray(encoding: String?=null):ByteArray {
    if(encoding==null) {
        return (this as java.lang.String).getBytes()
    } else {
        return (this as java.lang.String).getBytes(encoding)
    }
}
public inline fun String.toByteArray(encoding: java.nio.charset.Charset):ByteArray =  (this as java.lang.String).getBytes(encoding)

public inline fun String.toBoolean() : Boolean = java.lang.Boolean.parseBoolean(this)
public inline fun String.toShort() : Short = java.lang.Short.parseShort(this)
public inline fun String.toInt() : Int = java.lang.Integer.parseInt(this)
public inline fun String.toLong() : Long = java.lang.Long.parseLong(this)
public inline fun String.toFloat() : Float = java.lang.Float.parseFloat(this)
public inline fun String.toDouble() : Double = java.lang.Double.parseDouble(this)

/**
 * Converts the string into a regular expression [[Pattern]] optionally
 * with the specified flags from [[Pattern]] or'd together
 * so that strings can be split or matched on.
 */
public inline fun String.toRegex(flags: Int=0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)
}

inline val String.reader : StringReader
get() = StringReader(this)

inline val String.size : Int
get() = length()


/**
 * Returns a copy of this string capitalised if it is not empty or already starting with an uppper case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt capitalize
 */
public inline fun String.capitalize(): String {
    return if (notEmpty() && charAt(0).isLowerCase()) substring(0, 1).toUpperCase() + substring(1) else this
}

/**
 * Returns a copy of this string with the first letter lower case if it is not empty or already starting with a lower case letter, otherwise returns this
 *
 * @includeFunctionBody ../../test/StringTest.kt decapitalize
 */
public inline fun String.decapitalize(): String {
    return if (notEmpty() && charAt(0).isUpperCase()) substring(0, 1).toLowerCase() + substring(1) else this
}

/**
 * Repeats a given string n times.
 * When n < 0, IllegalArgumentException is thrown.
 * @includeFunctionBody ../../test/StringTest.kt repeat
 */
public inline fun String.repeat(n: Int): String {
    require(n >= 0, { "Cannot repeat string $n times" })

    var sb = StringBuilder()
    for (i in 1..n) {
        sb.append(this)
    }
    return sb.toString()
}
