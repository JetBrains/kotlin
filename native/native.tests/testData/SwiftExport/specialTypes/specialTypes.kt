// KIND: STANDALONE
// MODULE: SpecialTypes
// FILE: main.kt

fun getConstantString(): String = "Hello, World!"

var string: String = ""

fun getString(): String = string
fun setString(value: String) { string = value }
fun areStringsEqual(lhs: String, rhs: String): Boolean = lhs == rhs
fun areStringsTheSame(lhs: String, rhs: String): Boolean = lhs === rhs

val predefinedASCIIString = "Hello, World!"
fun isPredefinedASCIIString(str: String): Boolean = str == predefinedASCIIString

val predefinedBMPString = "Привет, Мир!"
fun isPredefinedBMPString(str: String): Boolean = str == predefinedBMPString

val predefinedUnicodeString = "👋, 🌎"
fun isPredefinedUnicodeString(str: String): Boolean = str == predefinedUnicodeString