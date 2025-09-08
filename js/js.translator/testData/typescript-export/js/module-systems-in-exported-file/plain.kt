// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: plain.kt

@file:JsExport

package foo


val prop = 10


class C(val x: Int) {
    fun doubleX() = x * 2
}


fun box(): String = "OK"

@JsExport.Default
fun justSomeDefaultExport() = "OK"