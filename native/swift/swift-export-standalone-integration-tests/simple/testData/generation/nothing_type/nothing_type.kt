// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

typealias Foo = Nothing

typealias OptionalNothing = Nothing?

fun meaningOfLife(): Nothing = TODO()

fun meaningOfLife(p: Nothing): Nothing = TODO()

var variable: Nothing = TODO()
val value: Nothing = TODO()

class Bar(val p: Nothing)

fun nullableNothingInput(input: Nothing?) = print("input is nil")
fun nullableNothingOutput(): Nothing? = null

var nullableNothingVariable: Nothing? = null

fun meaningOfLife(input: Int): Nothing? = null
fun meaningOfLife(input: Nothing?): String = "hello"
