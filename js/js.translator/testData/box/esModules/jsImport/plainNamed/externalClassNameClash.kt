// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
// FILE: a.kt
package a

@JsImport("./a.mjs")
external class A {
    fun foo(): String
}

@JsImport("./a.mjs")
external fun bar(): Int

@JsImport("./a.mjs")
external val prop: Int

// FILE: b.kt
package b

@JsImport("./b.mjs")
external class A {
    fun foo(): String
}

@JsImport("./b.mjs")
external fun bar(): Int

@JsImport("./b.mjs")
external val prop: Int

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