// EXPECTED_REACHABLE_NODES: 1284
// IGNORE_BACKEND: JS
// FILE: main.kt
@file:Suppress(
    "NESTED_CLASS_IN_EXTERNAL_INTERFACE",
    "WRONG_BODY_OF_EXTERNAL_DECLARATION",
    "INLINE_EXTERNAL_DECLARATION",
    "NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE",
    "DECLARATION_CANT_BE_INLINED",
)
package foo


@JsName("null")
external interface Foo {
    companion object {
        inline fun test(): String = "OK"
    }
}

fun box(): String {
    return Foo.test()
}

// FILE: lib.js
function Foo() {}