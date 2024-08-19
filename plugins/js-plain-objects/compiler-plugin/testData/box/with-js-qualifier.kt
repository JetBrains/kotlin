// FILE: Test.kt
@file:JsQualifier("foundry.applications.api")
package test

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface Options {
    val name: String
}

// FILE: main.kt
import test.Options

fun box(): String {
    val x = Options(name = "OK")
    return x.name
}