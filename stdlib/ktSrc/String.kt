package std

import java.io.StringReader

inline fun String.lastIndexOf(str: String)  = (this as java.lang.String).lastIndexOf(str)

inline fun String.lastIndexOf(ch: Char) = (this as java.lang.String).lastIndexOf(ch.toString())

inline fun String.equalsIgnoreCase(anotherString: String) = (this as java.lang.String).equalsIgnoreCase(anotherString)

inline fun String.indexOf(str : String) = (this as java.lang.String).indexOf(str)

inline fun String.matches(regex : String): Boolean = (this as java.lang.String).matches(regex)

inline fun String.indexOf(str : String, fromIndex : Int) = (this as java.lang.String).indexOf(str, fromIndex)

inline fun String.replace(oldChar: Char, newChar : Char) = (this as java.lang.String).replace(oldChar, newChar).sure()

inline fun String.replaceAll(regex: String, replacement : String) = (this as java.lang.String).replaceAll(regex, replacement).sure()

inline fun String.trim() = (this as java.lang.String).trim().sure()

/** Returns the string with leading and trailing text matching the given string removed */
inline fun String.trim(text: String) = trimLeading(text).trimTrailing(text)

/** Returns the string with the prefix and postfix text trimmed */
inline fun String.trim(prefix: String, postfix: String) = trimLeading(prefix).trimTrailing(postfix)

/** Returns the string with all leading occurrences of the given string removed */
inline fun String.trimLeading(prefix: String): String {
    var answer = this
    while (answer.startsWith(prefix)) {
        answer = answer.substring(prefix.length())
    }
    return answer
}

/** Returns the string with the all the trailing occurrences of the given string removed */
inline fun String.trimTrailing(postfix: String): String {
    var answer = this
    while (answer.endsWith(postfix)) {
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

/*
Iterator for characters of given CharSequence
*/
inline fun CharSequence.iterator() : CharIterator = object: jet.CharIterator() {
   private var index = 0

   public override fun nextChar(): Char = get(index++)

   public override val hasNext: Boolean
   get() = index < length
}
