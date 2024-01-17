// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.js.JsPlainObject

external interface A

external interface B

external interface C

@JsPlainObject
external interface D : <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>A<!>, <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>B<!>, <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>C<!>

@JsPlainObject
external interface E

@JsPlainObject
external interface F

@JsPlainObject
external interface DEF: D, E, F

external interface G: A, C, <!IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED!>E<!>

class Foo : A, <!IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED!>D<!>, B
