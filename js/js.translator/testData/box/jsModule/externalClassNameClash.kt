// EXPECTED_REACHABLE_NODES: 1284
// FILE: a.kt
// MODULE_KIND: AMD
@file:JsModule("a")
package a

external class A {
    fun foo(): String
}

external fun bar(): Int

external val prop: Int

// FILE: b.kt
// MODULE_KIND: AMD
@file:JsModule("b")
package b

external class A {
    fun foo(): String
}

external fun bar(): Int

external var prop: Int

// FILE: main.kt

import a.A as O
import b.A as K

fun box(): String {
    if (a.bar() != 1) return "fail 1"
    if (a.prop != 10) return "fail 2"
    if (b.bar() != 2) return "fail 3"
    if (b.prop != 20) return "fail 4"

    return O().foo() + K().foo()
}