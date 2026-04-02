// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.js.JsPlainObject

external interface Marker1
external interface Marker2

external interface A {
    val foo: String
}

external interface B {
    val foo: String
}

external interface C {
    val foo: String
}

external interface WrongMarker1 : C
external interface WrongMarker2 {
    val test: String
}

@JsPlainObject
external interface D : <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>A<!>, <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>B<!>, <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>C<!>

@JsPlainObject
external interface E

@JsPlainObject
external interface F : Marker1

@JsPlainObject
external interface DEF: D, E, F

@JsPlainObject
external interface G : Marker1, Marker2, <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>WrongMarker1<!>, <!JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS!>WrongMarker2<!>

external interface H: A, C, <!IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED!>E<!>

class Foo : A, <!IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED!>D<!>, B {
    override val foo = "Foo"
}

@JsPlainObject
external interface Parent {
    val name: Any
    val parentProperty: Any
}

@JsPlainObject
external interface Middle : Parent {
    override val parentProperty: Int
}

@JsPlainObject
external interface Child : Middle {
    override val name: String
}

fun parentChildTest(parent: Parent, middle: Middle, child: Child) {
    Parent(42, "Test")
    Middle(42, <!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)
    Child(<!ARGUMENT_TYPE_MISMATCH!>42<!>, <!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)

    Parent(42, 43)
    Middle(42, 43)
    Child(<!ARGUMENT_TYPE_MISMATCH!>42<!>, 43)

    Parent("Test", 43)
    Middle("Test", 43)
    Child("Test", 43)
}
