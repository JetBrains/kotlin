package kotlin

import java.io.StringReader
import java.util.List
import java.util.ArrayList

public inline fun String.lastIndexOf(str: String) : Int = (this as java.lang.String).lastIndexOf(str)

public inline fun String.lastIndexOf(ch: Char) : Int = (this as java.lang.String).lastIndexOf(ch.toString())

public inline fun String.equalsIgnoreCase(anotherString: String) : Boolean = (this as java.lang.String).equalsIgnoreCase(anotherString)

public inline fun String.indexOf(str : String) : Int = (this as java.lang.String).indexOf(str)

public inline fun String.indexOf(str : String, fromIndex : Int) : Int = (this as java.lang.String).indexOf(str, fromIndex)

public inline fun String.replace(oldChar: Char, newChar : Char) : String = (this as java.lang.String).replace(oldChar, newChar).sure()

public inline fun String.replaceAll(regex: String, replacement : String) : String = (this as java.lang.String).replaceAll(regex, replacement).sure()

public inline fun String.trim() : String = (this as java.lang.String).trim().sure()

/** Returns the string with leading and trailing text matching the given string removed */
public inline fun String.trim(text: String) : String = trimLeading(text).trimTrailing(text)

/** Returns the string with the prefix and postfix text trimmed */
public inline fun String.trim(prefix: String, postfix: String) : String = trimLeading(prefix).trimTrailing(postfix)

/** Returns the string with the leading prefix of this string removed */
public inline fun String.trimLeading(prefix: String): String {
    var answer = this
    if (answer.startsWith(prefix)) {
        answer = answer.substring(prefix.length())
    }
    return answer
}

/** Returns the string with the trailing postfix of this string removed */
public inline fun String.trimTrailing(postfix: String): String {
    var answer = this
    if (answer.endsWith(postfix)) {
        answer = answer.substring(0, length() - postfix.length())
    }
    return answer
}

public inline fun String.toUpperCase() : String = (this as java.lang.String).toUpperCase().sure()

public inline fun String.toLowerCase() : String = (this as java.lang.String).toLowerCase().sure()

public inline fun String.length() : Int = (this as java.lang.String).length()

public inline fun String.getBytes() : ByteArray = (this as java.lang.String).getBytes().sure()

public inline fun String.toCharArray() : CharArray = (this as java.lang.String).toCharArray().sure()

public inline fun String.toCharList(): List<Character> = toCharArray().toList()

public inline fun String.format(format : String, vararg args : Any?) : String = java.lang.String.format(format, args).sure()

public inline fun String.split(regex : String) : Array<String?>? = (this as java.lang.String).split(regex)

public inline fun String.substring(beginIndex : Int) : String = (this as java.lang.String).substring(beginIndex).sure()

public inline fun String.substring(beginIndex : Int, endIndex : Int) : String = (this as java.lang.String).substring(beginIndex, endIndex).sure()

public inline fun String.startsWith(prefix: String) : Boolean = (this as java.lang.String).startsWith(prefix)

public inline fun String.startsWith(prefix: String, toffset: Int) : Boolean = (this as java.lang.String).startsWith(prefix, toffset)

public inline fun String.contains(seq: CharSequence) : Boolean = (this as java.lang.String).contains(seq)

public inline fun String.endsWith(suffix: String) : Boolean = (this as java.lang.String).endsWith(suffix)

inline val String.size : Int
get() = length()

inline val String.reader : StringReader
get() = StringReader(this)

// "constructors" for String

public inline fun String(bytes : ByteArray, offset : Int, length : Int, charsetName : String) : String = java.lang.String(bytes, offset, length, charsetName) as String

public inline fun String(bytes : ByteArray, offset : Int, length : Int, charset : java.nio.charset.Charset) : String = java.lang.String(bytes, offset, length, charset) as String

public inline fun String(bytes : ByteArray, charsetName : String?) : String = java.lang.String(bytes, charsetName) as String

public inline fun String(bytes : ByteArray, charset : java.nio.charset.Charset) : String = java.lang.String(bytes, charset) as String

public inline fun String(bytes : ByteArray, i : Int, i1 : Int) : String = java.lang.String(bytes, i, i1) as String

public inline fun String(bytes : ByteArray) : String = java.lang.String(bytes) as String

public inline fun String(chars : CharArray) : String = java.lang.String(chars) as String

public inline fun String(stringBuffer : java.lang.StringBuffer) : String = java.lang.String(stringBuffer) as String

public inline fun String(stringBuilder : java.lang.StringBuilder) : String = java.lang.String(stringBuilder) as String

/** Returns true if the string is not null and not empty */
public inline fun String?.notEmpty() : Boolean = this != null && this.length() > 0

public inline fun String.toByteArray(encoding: String?=null):ByteArray {
    if(encoding==null) {
        return (this as java.lang.String).getBytes().sure()
    } else {
        return (this as java.lang.String).getBytes(encoding).sure()
    }
}
public inline fun String.toByteArray(encoding: java.nio.charset.Charset):ByteArray =  (this as java.lang.String).getBytes(encoding).sure()

public inline fun String.toBoolean() : Boolean = java.lang.Boolean.parseBoolean(this).sure()
public inline fun String.toShort() : Short = java.lang.Short.parseShort(this).sure()
public inline fun String.toInt() : Int = java.lang.Integer.parseInt(this).sure()
public inline fun String.toLong() : Long = java.lang.Long.parseLong(this).sure()
public inline fun String.toFloat() : Float = java.lang.Float.parseFloat(this).sure()
public inline fun String.toDouble() : Double = java.lang.Double.parseDouble(this).sure()

/**
 * Converts the string into a regular expression [[Pattern]] optionally
 * with the specified flags from [[Pattern]] or'd together
 * so that strings can be split or matched on.
 */
public inline fun String.toRegex(flags: Int=0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags).sure()
}

/**
Iterator for characters of given CharSequence
*/
public inline fun CharSequence.iterator() : CharIterator = object: jet.CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override val hasNext: Boolean
    get() = index < length
}

public inline fun String.replaceFirst(regex : String, replacement : String) : String = (this as java.lang.String).replaceFirst(regex, replacement).sure()

public inline fun String.charAt(index : Int) : Char = (this as java.lang.String).charAt(index).sure()

public inline fun String.split(regex : String, limit : Int) : Array<String?> = (this as java.lang.String).split(regex, limit).sure()

public inline fun String.codePointAt(index : Int) : Int = (this as java.lang.String).codePointAt(index).sure()

public inline fun String.codePointBefore(index : Int) : Int = (this as java.lang.String).codePointBefore(index).sure()

public inline fun String.codePointCount(beginIndex : Int, endIndex : Int) : Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

public inline fun String.compareToIgnoreCase(str : String) : Int = (this as java.lang.String).compareToIgnoreCase(str).sure()

public inline fun String.concat(str : String) : String = (this as java.lang.String).concat(str).sure()

public inline fun String.contentEquals(cs : CharSequence) : Boolean = (this as java.lang.String).contentEquals(cs).sure()

public inline fun String.contentEquals(sb : StringBuffer) : Boolean = (this as java.lang.String).contentEquals(sb).sure()

public inline fun String.getBytes(charset : java.nio.charset.Charset) : ByteArray = (this as java.lang.String).getBytes(charset).sure()

public inline fun String.getBytes(charsetName : String) : ByteArray = (this as java.lang.String).getBytes(charsetName).sure()

public inline fun String.getChars(srcBegin : Int, srcEnd : Int, dst : CharArray, dstBegin : Int) : Tuple0 = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin).sure()

public inline fun String.indexOf(ch : Char) : Int = (this as java.lang.String).indexOf(ch.toString()).sure()

public inline fun String.indexOf(ch : Char, fromIndex : Int) : Int = (this as java.lang.String).indexOf(ch.toString(), fromIndex).sure()

public inline fun String.intern() : String = (this as java.lang.String).intern().sure()

public inline fun String.isEmpty() : Boolean = (this as java.lang.String).isEmpty().sure()

public inline fun String.lastIndexOf(ch : Char, fromIndex : Int) : Int = (this as java.lang.String).lastIndexOf(ch.toString(), fromIndex).sure()

public inline fun String.lastIndexOf(str : String, fromIndex : Int) : Int = (this as java.lang.String).lastIndexOf(str, fromIndex).sure()

public inline fun String.matches(regex : String) : Boolean = (this as java.lang.String).matches(regex).sure()

public inline fun String.offsetByCodePoints(index : Int, codePointOffset : Int) : Int = (this as java.lang.String).offsetByCodePoints(index, codePointOffset).sure()

public inline fun String.regionMatches(ignoreCase : Boolean, toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len).sure()

public inline fun String.regionMatches(toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(toffset, other, ooffset, len).sure()

public inline fun String.replace(target : CharSequence, replacement : CharSequence) : String = (this as java.lang.String).replace(target, replacement).sure()

public inline fun String.subSequence(beginIndex : Int, endIndex : Int) : CharSequence = (this as java.lang.String).subSequence(beginIndex, endIndex).sure()

public inline fun String.toLowerCase(locale : java.util.Locale) : String = (this as java.lang.String).toLowerCase(locale).sure()

public inline fun String.toUpperCase(locale : java.util.Locale) : String = (this as java.lang.String).toUpperCase(locale).sure()

/** Returns the string if it is not null or the empty string if its null */
public inline fun String?.orEmpty(): String = this ?: ""


// "Extension functions" for CharSequence

public inline fun CharSequence.length() : Int = (this as java.lang.CharSequence).length()

inline val CharSequence.size : Int
get() = length()

public inline fun CharSequence.charAt(index : Int) : Char = (this as java.lang.CharSequence).charAt(index)

public inline fun CharSequence.get(index : Int) : Char = charAt(index)

public inline fun CharSequence.subSequence(start : Int, end : Int) : CharSequence? = (this as java.lang.CharSequence).subSequence(start, end)

public inline fun CharSequence.get(start : Int, end : Int) : CharSequence? = subSequence(start, end)

public inline fun CharSequence.toString() : String? = (this as java.lang.CharSequence).toString()