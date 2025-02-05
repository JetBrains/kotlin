// KIND: STANDALONE
// MODULE: NullType
// FILE: input.kt

class Bar
fun foo(a: Bar): String = "nonoptional"
fun foo(a: Bar?): String = "optional"
fun foo(): Bar? = null

var nullableBar: Bar? = null

class Foo(val nullableBar: Bar?)

typealias OptionalRef = Bar?
typealias OptOptRef = OptionalRef?

fun p_opt_opt_typealias(input: OptOptRef?): OptOptRef? = input
fun p_opt_typealias(input: OptOptRef): OptOptRef = input

var nullableAny: Any? = null
fun foo_any(a: Any): String = "nonoptional"
fun foo_any(a: Any?): String = "optional"

var optionalString: String? = null
fun strIdentity(str: String?): String? = str

fun meaningOfLife(input: Int): Nothing? = if (input == 42) null else TODO("try again")
fun meaningOfLife(input: Nothing?): String = "optional nothing received"

var meaningOfLife: Nothing? = null

fun multiple_nothings(arg1: Nothing?, arg2: Int, arg3: Nothing?): Nothing? = null


typealias OptionalInt = Int?
var optionalInt: OptionalInt = null
fun intIdentity(input: Int?): Int? = input
fun doubleIdentity(input: Double?): Double? = input

fun String?.extPrint(): String = this ?: "<null>"

val String?.extPrintProp get() = this ?: "<null>"