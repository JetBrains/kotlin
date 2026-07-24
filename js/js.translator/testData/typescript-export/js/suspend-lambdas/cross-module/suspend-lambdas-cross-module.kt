// ES_MODULES
// CHECK_TYPESCRIPT_DECLARATIONS
// DISABLE_JS_EXPORT_SOURCE_PREPROCESSOR
// LANGUAGE: +JsAllowExportingSuspendFunctions +JsExportingSuspendLambdas
// TSC_TARGET: es2020

// MODULE: lib
// FILE: lib.kt
package lib

@JsExport
val libSuspendLambda: suspend (Int) -> Int = { x -> x * 3 }

@JsExport
fun produceLibSuspendLambda(): suspend (Int) -> Int = { x -> x + 4 }

// MODULE: main(lib)
// FILE: main.kt
package main

import lib.produceLibSuspendLambda

@JsExport
val mainSuspendLambda: suspend () -> String = { "MAIN" }

@JsExport
fun reuseLibSuspendLambda(): suspend (Int) -> Int = produceLibSuspendLambda()

@JsExport
suspend fun callMainLambda(callback: suspend () -> String): String = callback()

@JsExport
suspend fun callIntLambda(callback: suspend (Int) -> Int, x: Int): Int = callback(x)

fun box(): String = "OK"
