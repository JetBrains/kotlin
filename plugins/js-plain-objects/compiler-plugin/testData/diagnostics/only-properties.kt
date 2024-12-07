// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface Foo {
    val foo: String
    val bar: Int?
    val fn: () -> String
    val fnOptional: (() -> String)?
    
    <!METHODS_ARE_NOT_ALLOWED_INSIDE_JS_PLAIN_OBJECT!>fun test(): String<!>
}
