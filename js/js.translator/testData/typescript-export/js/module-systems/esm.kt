// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// JS_MODULE_KIND: ES
// WITH_STDLIB
// FILE: esm.kt

package foo

import kotlin.js.Promise

@JsExport
val value = 10

@JsExport
var variable = 10

@JsExport
class C(val x: Int) {
    fun doubleX() = x * 2
}

@JsExport
object O {
    val value = 10
}

@JsExport
object Parent {
    val value = 10
    class Nested {
        val value = 10
    }
}

@JsExport
interface AnInterfaceWithCompanion {
    companion object {
        val someValue = "OK"
    }
}

@JsExport
fun box(): String = "OK"

@JsExport
fun asyncList(): Promise<List<Int>> =
    Promise.resolve(listOf(1, 2))

@JsExport
fun arrayOfLists(): Array<List<Int>> =
    arrayOf(listOf(1, 2))

@JsExport.Default
fun justSomeDefaultExport() = "OK"
