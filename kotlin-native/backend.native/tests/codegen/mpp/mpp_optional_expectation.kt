@file:Suppress("EXPERIMENTAL_API_USAGE_ERROR")

import kotlin.js.*

@OptionalExpectation
expect annotation class Optional()

@Optional
fun foo() { println(42) }

@JsName("jsBar")
fun bar() { println(43) }

fun main(args: Array<String>) {
    foo()
    bar()
}