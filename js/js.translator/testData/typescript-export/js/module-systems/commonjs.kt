// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// JS_MODULE_KIND: COMMON_JS
// WITH_STDLIB
// FILE: commonjs.kt

package foo

import kotlin.js.Promise

@JsExport
val prop = 10

@JsExport
class C(val x: Int) {
    fun doubleX() = x * 2
}

@JsExport
fun box(): String = "OK"

@JsExport
fun asyncList(): Promise<List<Int>> =
    Promise.resolve(listOf(1, 2))

@JsExport
fun arrayOfLists(): Array<List<Int>> =
    arrayOf(listOf(1, 2))

@JsExport
fun acceptArrayOfPairs(array: Array<Pair<String, String>>) {}

@JsExport.Default
fun justSomeDefaultExport() = "OK"
