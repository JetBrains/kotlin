// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
// FILE: a.kt
@file:JsModule("./a.mjs")
package a

external class A {
    fun foo(): String
}

external fun bar(): Int

external val prop: Int

// FILE: b.kt
@file:JsModule("./b.mjs")
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

// FILE: a.mjs
export function A() {}

A.prototype.foo = function () {
   return "O";
};

export function bar() { return 1; }

export let prop = 10


// FILE: b.mjs
export function A() {}

A.prototype.foo = function () {
    return "K";
};

export function bar() { return 2; }

export let prop = 20;