// DONT_TARGET_EXACT_BACKEND: JS
// MODULE_KIND: AMD
// FILE: a.kt
package a

@JsImport("a")
external class A {
    fun foo(): String
}

@JsImport("a")
external fun bar(): Int

@JsImport("a")
external val prop: Int

// FILE: b.kt
package b

@JsImport("b")
external class A {
    fun foo(): String
}

@JsImport("b")
external fun bar(): Int

@JsImport("b")
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

// FILE: a.js
define("a", [], function () {
    function A() {}

    A.prototype.foo = function () {
        return "O";
    };

    function bar() { return 1; }

    let prop = 10

    return { A, bar, prop }
})

// FILE: b.js
define("b", [], function () {
    function A() {}

    A.prototype.foo = function () {
        return "K";
    };

    function bar() { return 2; }

    let prop = 20;

    return { A, bar, prop }
})