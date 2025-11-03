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

// KT-79926
@JsExport
interface AnInterfaceWithCompanion {
    companion object {
        private val privateValue = "OK"
        val someValue = privateValue
        const val constValue = "OK"
    }
}

// Should be uncommented when KT-82128 is done
//@JsExport
//interface InterfaceWithNamedCompanion {
//    companion object Name {
//        private val privateValue = "OK"
//        val someValue = privateValue
//        const val constValue = "OK"
//    }
//}

@JsExport
interface InterfaceWithCompanionWithStaticFun {
    companion object {
        @JsStatic
        fun bar() = "OK"
    }
}

@JsExport
interface I {
    fun foo(): String
}

@JsExport
interface InterfaceWithCompanionWithInheritor {
    companion object : I {
        override fun foo(): String = "OK"
    }
}

@JsExport
interface InterfaceWithCompanionWithInheritorAndStaticFun {
    companion object : I {
        @JsStatic
        override fun foo(): String = "OK"
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
