// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.jso.JsSimpleObject

external interface A

external interface B

external interface C

@JsSimpleObject
external interface D : A, B, C

@JsSimpleObject
external interface E

@JsSimpleObject
external interface F

@JsSimpleObject
external interface DEF: D, E, F

external interface G: A, C, <!IMPLEMENTING_OF_JSO_IS_NOT_SUPPORTED!>E<!>

class Foo : A, <!IMPLEMENTING_OF_JSO_IS_NOT_SUPPORTED!>D<!>, B
