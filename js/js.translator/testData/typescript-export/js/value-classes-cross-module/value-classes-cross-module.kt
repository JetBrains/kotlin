// ES_MODULES
// CHECK_TYPESCRIPT_DECLARATIONS
// DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR

// MODULE: lib
// FILE: lib.kt
package lib

@JsExport
value class LibValue(val value: Int)

@JsExport.Default
value class DefaultValueClass(val value: String)

@JsExport
fun createLibValue(value: Int): LibValue = LibValue(value)

@JsExport
fun echoDefaultValueClass(value: DefaultValueClass): DefaultValueClass = value

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.LibValue
import lib.createLibValue

@JsExport
fun echoLibValue(value: LibValue): LibValue = value

@JsExport
fun createMainValue(value: Int): LibValue = createLibValue(value)

@JsExport
external interface MainExternalInterface {
    val directValue: LibValue
    val nullableValue: LibValue?

    fun echo(value: LibValue): LibValue
}

fun box(): String = "OK"
