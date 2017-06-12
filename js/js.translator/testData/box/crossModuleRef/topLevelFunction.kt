// EXPECTED_REACHABLE_NODES: 493
// MODULE: lib
// FILE: lib.kt

package lib

fun foo() = 23

external fun bar(): Int = definedExternally

inline fun baz() = 99

inline fun callFoo() = foo()

inline fun buzz(): Int {
    val o = object {
        fun f() = 111
    }
    return o.f()
}

// FILE: lib.js

function bar() {
    return 42;
}

// MODULE: main(lib)
// FILE: main.kt

package main

fun box(): String {
    val a = lib.foo()
    if (a != 23) return "fail: simple function: $a"

    val b = lib.bar()
    if (b != 42) return "fail: native function: $b"

    val c = lib.baz()
    if (c != 99) return "fail: inline function: $c"

    val d = lib.buzz()
    if (d != 111) return "fail: inline function with object expression: $d"

    val e = lib.callFoo()
    if (e != 23) return "fail: inline function calling another function: $e"

    return "OK"
}