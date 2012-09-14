package js

native public fun String.toUpperCase() : String = js.noImpl

native public fun String.toLowerCase() : String = js.noImpl

native public fun String.indexOf(str : String) : Int = js.noImpl
native public fun String.indexOf(str : String, fromIndex : Int) : Int = js.noImpl

native public fun String.lastIndexOf(str: String) : Int = js.noImpl
native public fun String.lastIndexOf(str : String, fromIndex : Int) : Int = js.noImpl

// Defined in javalang.kt
//native public fun String.split(regex : String) : Array<String> = js.noImpl

native public fun String.substring(beginIndex : Int) : String = js.noImpl
native public fun String.substring(beginIndex : Int, endIndex : Int) : String = js.noImpl

native public fun String.charAt(index : Int) : Char = js.noImpl

native public fun String.concat(str : String) : String = js.noImpl

native public fun String.match(regex : String) : Array<String> = js.noImpl

native public fun String.trim() : String = js.noImpl

native public val String.length : Int
    get() = js.noImpl


/*

native public fun String.equalsIgnoreCase(anotherString: String) : Boolean = (this as java.lang.String).equalsIgnoreCase(anotherString)

native public fun String.hashCode() : Int = (this as java.lang.String).hashCode()

native public fun String.replace(oldChar: Char, newChar : Char) : String = (this as java.lang.String).replace(oldChar, newChar)!!

native public fun String.replaceAll(regex: String, replacement : String) : String = (this as java.lang.String).replaceAll(regex, replacement)!!


native public fun String.length() : Int = (this as java.lang.String).length()

native public fun String.getBytes() : ByteArray = (this as java.lang.String).getBytes()!!

native public fun String.toCharArray() : CharArray = (this as java.lang.String).toCharArray()!!

native public fun String.toCharList(): List<Char> = toCharArray().toList()

native public fun String.format(format : String, vararg args : Any?) : String = java.lang.String.format(format, args)!!


native public fun String.split(ch : Char) : Array<String> = (this as java.lang.String).split(java.util.regex.Pattern.quote(ch.toString())) as Array<String>

native public fun String.startsWith(prefix: String) : Boolean = (this as java.lang.String).startsWith(prefix)

native public fun String.startsWith(prefix: String, toffset: Int) : Boolean = (this as java.lang.String).startsWith(prefix, toffset)

native public fun String.startsWith(ch: Char) : Boolean = (this as java.lang.String).startsWith(ch.toString())

native public fun String.contains(seq: CharSequence) : Boolean = (this as java.lang.String).contains(seq)

native public fun String.endsWith(suffix: String) : Boolean = (this as java.lang.String).endsWith(suffix)

native public fun String.endsWith(ch: Char) : Boolean = (this as java.lang.String).endsWith(ch.toString())

// "constructors" for String

native public fun String(bytes : ByteArray, offset : Int, length : Int, charsetName : String) : String = java.lang.String(bytes, offset, length, charsetName) as String

native public fun String(bytes : ByteArray, offset : Int, length : Int, charset : java.nio.charset.Charset) : String = java.lang.String(bytes, offset, length, charset) as String

native public fun String(bytes : ByteArray, charsetName : String?) : String = java.lang.String(bytes, charsetName) as String

native public fun String(bytes : ByteArray, charset : java.nio.charset.Charset) : String = java.lang.String(bytes, charset) as String

native public fun String(bytes : ByteArray, i : Int, i1 : Int) : String = java.lang.String(bytes, i, i1) as String

native public fun String(bytes : ByteArray) : String = java.lang.String(bytes) as String

native public fun String(chars : CharArray) : String = java.lang.String(chars) as String

native public fun String(stringBuffer : java.lang.StringBuffer) : String = java.lang.String(stringBuffer) as String

native public fun String(stringBuilder : java.lang.StringBuilder) : String = java.lang.String(stringBuilder) as String

native public fun String.replaceFirst(regex : String, replacement : String) : String = (this as java.lang.String).replaceFirst(regex, replacement)!!


native public fun String.split(regex : String, limit : Int) : Array<String?> = (this as java.lang.String).split(regex, limit)!!

native public fun String.codePointAt(index : Int) : Int = (this as java.lang.String).codePointAt(index)!!

native public fun String.codePointBefore(index : Int) : Int = (this as java.lang.String).codePointBefore(index)!!

native public fun String.codePointCount(beginIndex : Int, endIndex : Int) : Int = (this as java.lang.String).codePointCount(beginIndex, endIndex)

native public fun String.compareToIgnoreCase(str : String) : Int = (this as java.lang.String).compareToIgnoreCase(str)!!


native public fun String.contentEquals(cs : CharSequence) : Boolean = (this as java.lang.String).contentEquals(cs)!!

native public fun String.contentEquals(sb : StringBuffer) : Boolean = (this as java.lang.String).contentEquals(sb)!!

native public fun String.getBytes(charset : java.nio.charset.Charset) : ByteArray = (this as java.lang.String).getBytes(charset)!!

native public fun String.getBytes(charsetName : String) : ByteArray = (this as java.lang.String).getBytes(charsetName)!!

native public fun String.getChars(srcBegin : Int, srcEnd : Int, dst : CharArray, dstBegin : Int) : Tuple0 = (this as java.lang.String).getChars(srcBegin, srcEnd, dst, dstBegin)!!

native public fun String.intern() : String = (this as java.lang.String).intern()!!

native public fun String.isEmpty() : Boolean = (this as java.lang.String).isEmpty()!!


native public fun String.offsetByCodePoints(index : Int, codePointOffset : Int) : Int = (this as java.lang.String).offsetByCodePoints(index, codePointOffset)!!

native public fun String.regionMatches(ignoreCase : Boolean, toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(ignoreCase, toffset, other, ooffset, len)!!

native public fun String.regionMatches(toffset : Int, other : String, ooffset : Int, len : Int) : Boolean = (this as java.lang.String).regionMatches(toffset, other, ooffset, len)!!

native public fun String.replace(target : CharSequence, replacement : CharSequence) : String = (this as java.lang.String).replace(target, replacement)!!

native public fun String.subSequence(beginIndex : Int, endIndex : Int) : CharSequence = (this as java.lang.String).subSequence(beginIndex, endIndex)!!

native public fun String.toLowerCase(locale : java.util.Locale) : String = (this as java.lang.String).toLowerCase(locale)!!

native public fun String.toUpperCase(locale : java.util.Locale) : String = (this as java.lang.String).toUpperCase(locale)!!


native public fun CharSequence.charAt(index : Int) : Char = (this as java.lang.CharSequence).charAt(index)

native public fun CharSequence.get(index : Int) : Char = charAt(index)

native public fun CharSequence.subSequence(start : Int, end : Int) : CharSequence? = (this as java.lang.CharSequence).subSequence(start, end)

native public fun CharSequence.get(start : Int, end : Int) : CharSequence? = subSequence(start, end)

native public fun CharSequence.toString() : String? = (this as java.lang.CharSequence).toString()

native public fun String.toByteArray(encoding: String?=null):ByteArray {
    if(encoding==null) {
        return (this as java.lang.String).getBytes()!!
    } else {
        return (this as java.lang.String).getBytes(encoding)!!
    }
}
native public fun String.toByteArray(encoding: java.nio.charset.Charset):ByteArray =  (this as java.lang.String).getBytes(encoding)!!

native public fun String.toBoolean() : Boolean = java.lang.Boolean.parseBoolean(this)!!
native public fun String.toShort() : Short = java.lang.Short.parseShort(this)!!
native public fun String.toInt() : Int = java.lang.Integer.parseInt(this)!!
native public fun String.toLong() : Long = java.lang.Long.parseLong(this)!!
native public fun String.toFloat() : Float = java.lang.Float.parseFloat(this)!!
native public fun String.toDouble() : Double = java.lang.Double.parseDouble(this)!!

native public fun String.toRegex(flags: Int=0): java.util.regex.Pattern {
    return java.util.regex.Pattern.compile(this, flags)!!
}
*/