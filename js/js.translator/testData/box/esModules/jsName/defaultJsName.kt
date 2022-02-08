// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
// MODULE: main
// FILE: lib.kt
@file:JsModule("./foo.mjs")
package lib

@JsName("default")
external val foo: Int

// FILE: lib2.kt
@file:JsModule("./bar.mjs")
package lib

@JsName("default")
external fun foo(): Int

// FILE: main.kt
package main

import lib.*

fun box(): String {
    if (foo != 23 && foo() != 23) return "fail"
    return "OK"
}

// FILE: foo.mjs

export default 23;

// FILE: bar.mjs

export default function() { return 23; };