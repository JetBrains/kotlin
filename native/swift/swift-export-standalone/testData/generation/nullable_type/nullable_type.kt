// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

class Bar

fun foo(a: Bar): Unit = TODO()
fun foo(a: Bar?): Unit = TODO()

fun foo_any(a: Any): Unit = TODO()
fun foo_any(a: Any?): Unit = TODO()

fun p(): Bar? = null
fun p_any(): Any? = null

class Foo(b: Bar?) {
    var variable: Bar? = null
    val value: Bar? = null
    val any_value: Any? = null
    fun accept(b: Bar?): Unit = TODO()
    fun produce(): Bar? = TODO()
}

typealias NonoptionalRef = Bar
typealias OptToNonOptTypealias = NonoptionalRef?

fun opt_to_non_opt_usage(i: OptToNonOptTypealias?): Unit = TODO()

typealias OptionalRef = Bar?
typealias OptOptRef = OptionalRef?

fun p_opt_opt_out(): OptOptRef? = null
fun p_opt_opt_in(input: OptOptRef?) = Unit

fun string_in(a: String?): Unit = TODO()
fun string_out(): String? = null
var str: String? = null

fun primitive_in(
    arg1: Boolean?,
    arg2: Byte?,
    arg3: Short?,
    arg4: Int?,
    arg5: Long?,
    arg6: UByte?,
    arg7: UShort?,
    arg8: UInt?,
    arg9: ULong?,
    arg10: Float?,
    arg11: Double?,
    arg12: Char?,
): Unit = TODO()
fun primitive_out(): Boolean? = null
var primitive: Double? = null

