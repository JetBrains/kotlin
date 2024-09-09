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
