// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// JS_MODULE_KIND: ES
// WITH_STDLIB
// FILE: esm.kt

@file:JsExport

package foo

import kotlin.js.Promise


val value = 10


var variable = 10


class C(val x: Int) {
    fun doubleX() = x * 2
}


object O {
    val value = 10
}


object Parent {
    val value = 10
    class Nested {
        val value = 10
    }
}


interface AnInterfaceWithCompanion {
    companion object {
        val someValue = "OK"
    }
}


fun box(): String = "OK"


fun asyncList(): Promise<List<Int>> =
    Promise.resolve(listOf(1, 2))


fun arrayOfLists(): Array<List<Int>> =
    arrayOf(listOf(1, 2))

@JsExport.Default
fun justSomeDefaultExport() = "OK"