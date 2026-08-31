// LANGUAGE: +FullValueClasses
// ES_MODULES
// CHECK_TYPESCRIPT_DECLARATIONS
// DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR

// MODULE: lib
// FILE: lib.kt
package lib

@JsExport
value class LibPoint(val x: Int, val y: Int) {
    fun sum(): Int = x + y
}

@JsExport
fun createLibPoint(x: Int, y: Int): LibPoint = LibPoint(x, y)

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.LibPoint
import lib.createLibPoint

@JsExport
fun echoLibPoint(point: LibPoint): LibPoint = point

@JsExport
fun createMainPoint(x: Int, y: Int): LibPoint = createLibPoint(x, y)

fun box(): String = "OK"
