// MODULE: main
// MODULE_KIND: COMMON_JS
// FILE: lib.kt
@file:JsModule("foo")
package lib

@JsName("eval")
external val foo: Int


// FILE: main.kt
package main

import lib.*

fun box(): String {
    if (foo != 23 || eval("false")) return "fail"
    return "OK"
}

// FILE: hello.js

$kotlin_test_internal$.beginModule("foo");
module.exports = {
    "eval": 23
}
$kotlin_test_internal$.endModule("foo");
