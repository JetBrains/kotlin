// EXPECTED_REACHABLE_NODES: 494
// MODULE: lib
// FILE: lib.kt
package lib

val foo = 23

val boo: Int
    get() = 42

external val bar: Int = definedExternally

external val far: Int
    get() = definedExternally

// TODO: annotations like this are not serialized properly. Uncomment after KT-14529 gets fixed
/*
val fuzz: Int
    @JsName("getBuzz") get() = 55
    */

inline fun fetchFoo() = foo

@JsName("fee")
val tee = 2525

// FILE: lib.js

var bar = 99
var far = 111

// MODULE: main(lib)
// FILE: lib.kt
package main

import lib.*

fun box(): String {
    if (foo != 23) return "fail: simple property: $foo"
    if (boo != 42) return "fail: property with accessor: $boo"
    if (bar != 99) return "fail: native property: $bar"
    if (far != 111) return "fail: native property with accessor: $far"
    //if (fuzz != 55) return "fail: property with JsName on accessor: $fuzz"
    if (tee != 2525) return "fail: native property with JsName: $tee"

    return "OK"
}
