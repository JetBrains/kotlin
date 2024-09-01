// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

typealias Foo = Nothing

typealias OptionalNothing = Nothing? // we do not support optionals currently - will not be translated.

fun meaningOfLife(): Nothing = TODO()

fun meaningOfLife(p: Nothing): Nothing = TODO()

// impossible construct on Kotlin Side - 'Nothing' return type can't be specified with type alias
//fun meaningOfLife(): Foo = TODO()

var variable: Nothing = TODO()
val value: Nothing = TODO()

class Bar(val p: Nothing)
