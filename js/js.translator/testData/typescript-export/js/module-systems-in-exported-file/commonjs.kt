// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// JS_MODULE_KIND: COMMON_JS
// WITH_STDLIB
// FILE: commonjs.kt

@file:JsExport

package foo

import kotlin.js.Promise


val prop = 10


class C(val x: Int) {
    fun doubleX() = x * 2
}


fun box(): String = "OK"


fun asyncList(): Promise<List<Int>> =
    Promise.resolve(listOf(1, 2))


fun arrayOfLists(): Array<List<Int>> =
    arrayOf(listOf(1, 2))


fun acceptArrayOfPairs(array: Array<Pair<String, String>>) {}

@JsExport.Default
fun justSomeDefaultExport() = "OK"