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
