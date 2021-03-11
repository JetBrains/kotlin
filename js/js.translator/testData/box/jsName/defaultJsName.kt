// MODULE: main
// MODULE_KIND: COMMON_JS
// FILE: lib.kt
@file:JsModule("foo")
package lib

@JsName("default")
external val foo: Int

@JsName("for")
external val bar: String

// FILE: lib2.kt
@file:JsModule("bar")
package lib

@JsName("default")
external fun foo(): Int

// FILE: main.kt
package main

import lib.*

fun box(): String {
    if (foo != 23 || bar != "hello" || foo() != 23) return "fail"
    return "OK"
}

// FILE: hello.js

$kotlin_test_internal$.beginModule("foo");
module.exports = {
    "default": 23,
    "for": "hello"
}
$kotlin_test_internal$.endModule("foo");

$kotlin_test_internal$.beginModule("bar");
module.exports = {
    "default": function() {
        return 23
    }
}
$kotlin_test_internal$.endModule("bar");
