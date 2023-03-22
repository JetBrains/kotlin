// DONT_TARGET_EXACT_BACKEND: JS
// KJS_WITH_FULL_RUNTIME

// MODULE: ModuleA1
// NO_COMMON_FILES
// FILE: modulea1.kt
package demoPackage.a1

@JsExport
data class ModuleA1Class(private val string: String = "A1") {
    override fun toString(): String = string
}

// MODULE: ModuleA2(ModuleA1)
// NO_COMMON_FILES
// FILE: modulea2.kt
package demoPackage.a2

import demoPackage.a1.ModuleA1Class

@JsExport
fun moduleA2Function() = ModuleA1Class("A2")

// MODULE: ModuleB
// NO_COMMON_FILES
// FILE: moduleb.kt
package demoPackage.b

@JsExport
fun moduleBFunction() = "B"

// MODULE: main(ModuleA2, ModuleB)
// MODULE_KIND: COMMON_JS
// FILE: main.kt

package mainPackage

import demoPackage.*

external interface JsResult {
    val moduleA1: String
    val moduleA2: String
    val moduleB: String
}

@JsModule("lib")
external fun jsBox(): JsResult

fun box(): String {
    assertEquals(demoPackage.b.moduleBFunction(), "B")

    val res = jsBox()
    assertEquals(res.moduleA1, "A1")
    assertEquals(res.moduleA2, "A2")
    assertEquals(res.moduleB, "B")
    return "OK"
}