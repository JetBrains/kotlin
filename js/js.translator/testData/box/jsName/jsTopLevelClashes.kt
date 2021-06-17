// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_OLD_MODULE_SYSTEMS
// MODULE: main
// FILE: lib.kt
@file:JsModule("./foo.mjs")
package lib

@JsName("test")
external val foo: Int

// FILE: lib2.kt
@file:JsModule("./bar.mjs")
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

// FILE: foo.mjs
export var test = 23;

// FILE: bar.mjs
export var test = 45;