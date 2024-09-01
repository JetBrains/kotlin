// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

fun getConstantString(): String = "Hello, World!"

var string: String = getConstantString()

fun getString(): String = string
fun setString(value: String) { string = value }