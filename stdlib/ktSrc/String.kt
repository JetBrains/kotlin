package std

import java.io.StringReader

inline fun <T> T?.plus(str: String?) : String { return toString() + str }

inline fun String.lastIndexOf(s: String)  = (this as java.lang.String).lastIndexOf(s)

inline fun String.lastIndexOf(s: Char) = (this as java.lang.String).lastIndexOf(s.toString())

inline fun String.equalsIgnoreCase(s: String) = (this as java.lang.String).equalsIgnoreCase(s)

inline fun String.indexOf(s : String) = (this as java.lang.String).indexOf(s)

inline fun String.indexOf(p0 : String, p1 : Int) = (this as java.lang.String).indexOf(p0, p1)

inline fun String.replace(s: Char, s1 : Char) = (this as java.lang.String).replace(s, s1).sure()

inline fun String.replaceAll(s: String, s1 : String) = (this as java.lang.String).replaceAll(s, s1).sure()

inline fun String.trim() = (this as java.lang.String).trim().sure()

inline fun String.toUpperCase() = (this as java.lang.String).toUpperCase().sure()

inline fun String.toLowerCase() = (this as java.lang.String).toLowerCase().sure()

inline fun String.length() = (this as java.lang.String).length()

inline fun String.getBytes() = (this as java.lang.String).getBytes().sure()

inline fun String.toCharArray() = (this as java.lang.String).toCharArray().sure()

inline fun String.format(s : String, vararg objects : Any?)  = java.lang.String.format(s, objects).sure()

inline fun String.split(s : String)  = (this as java.lang.String).split(s)

inline fun String.substring(i : Int) = (this as java.lang.String).substring(i).sure()

inline fun String.substring(i0 : Int, i1 : Int)  = (this as java.lang.String).substring(i0, i1).sure()

inline fun String.startsWith(prefix: String) = (this as java.lang.String).startsWith(prefix)

inline fun String.startsWith(prefix: String, toffset: Int) = (this as java.lang.String).startsWith(prefix, toffset)

inline fun String.contains(seq: CharSequence) : Boolean = (this as java.lang.String).contains(seq)

inline fun String.endsWith(seq: String) : Boolean = (this as java.lang.String).endsWith(seq)

inline val String.size : Int
    get() = length()

inline val String.reader : StringReader
    get() = StringReader(this)

// "constructors" for String

inline fun String(bytes : ByteArray, i : Int, i1 : Int, s : String) = java.lang.String(bytes, i, i1, s) as String

inline fun String(bytes : ByteArray, i : Int, i1 : Int, charset : java.nio.charset.Charset) = java.lang.String(bytes, i, i1, charset) as String

inline fun String(bytes : ByteArray, s : String?) = java.lang.String(bytes, s) as String

inline fun String(bytes : ByteArray, charset : java.nio.charset.Charset) = java.lang.String(bytes, charset) as String

inline fun String(bytes : ByteArray, i : Int, i1 : Int) = java.lang.String(bytes, i, i1) as String

inline fun String(bytes : ByteArray) = java.lang.String(bytes) as String

inline fun String(chars : CharArray) = java.lang.String(chars) as String

inline fun String(stringBuffer : java.lang.StringBuffer) = java.lang.String(stringBuffer) as String

inline fun String(stringBuilder : java.lang.StringBuilder) = java.lang.String(stringBuilder) as String

/*
Iterator for characters of given CharSequence
*/
inline fun CharSequence.iterator() : CharIterator = object: jet.CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override val hasNext: Boolean
        get() = index < length
}
