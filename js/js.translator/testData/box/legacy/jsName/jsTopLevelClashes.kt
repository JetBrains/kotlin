// SKIP_ES_MODULES
// MODULE: main
// MODULE_KIND: COMMON_JS
// FILE: lib.kt
@file:JsModule("foo")
package lib

@JsName("test")
external val foo: Int

// FILE: lib2.kt
@file:JsModule("bar")
package lib

@JsName("test")
external val bar: Int

// FILE: main.kt
package main

import lib.*

fun box(): String {
    if (foo != 23 || bar != 45) return "fail"
    return "OK"
}

// FILE: hello.js

$kotlin_test_internal$.beginModule("foo");
module.exports = {
    "test": 23
}
$kotlin_test_internal$.endModule("foo");

$kotlin_test_internal$.beginModule("bar");
module.exports = {
    "test": 45
}
$kotlin_test_internal$.endModule("bar");
