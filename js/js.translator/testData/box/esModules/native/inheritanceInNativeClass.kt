// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES

// FILE: main.kt

@JsExport
open class A {
    @JsName("foo")
    open protected fun foo(n: Int) = 23

    @JsName("bar")
    fun bar(n: Int) = foo(n) + 100
}

@JsExport
open class B {
    @JsName("foo")
    protected fun foo(n: Int) = 42

    @JsName("bar")
    open fun bar(n: Int) = 142
}

// FILE: entry.mjs
// ENTRY_ES_MODULE

import { A, B } from "./inheritanceInNativeClass_v5.mjs";

function createA() {
    function ADerived() {
    }
    ADerived.prototype = Object.create(A.prototype);
    ADerived.prototype.foo = function(n) {
        return 24;
    };
    return new ADerived();
}

function createB() {
    function BDerived() {
    }
    BDerived.prototype = Object.create(B.prototype);
    BDerived.prototype.bar = function(n) {
        return this.foo(n);
    };
    return new BDerived();
}

export function box() {
    let a = createA();
    if (a.bar(0) != 124) return "fail1";

    let b = createB();
    if (b.bar(0) != 42) return "fail2";

    return "OK";
}