// KIND: STANDALONE
// MODULE: Char
// FILE: char.kt

private val string = "AB0-Ыß☺🙂系"

fun getCharAt(index: Int): Char = string.get(index)

fun isEqualToCharAt(c: Char, index: Int): Boolean = (c == string.get(index))

var optionalChar: Char? = null
fun charAtIndexOrNull(str: String, index: Int): Char? = runCatching { str.get(index) }.getOrNull()
