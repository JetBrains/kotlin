// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: data-classes.kt

@file:JsExport

package foo



data class TestDataClass(val name: String) {
    class Nested {
        val prop: String = "hello"
    }
}


data class KT39423(
    val a: String,
    val b: Int? = null
)


abstract class WithComponent1 {
    abstract fun component1(): String
}

@JsExport.Ignore
abstract class NonExportedWithComponent1 {
    abstract fun component1(): String
}


data class Test2(val value1: String, val value2: String): WithComponent1()


data class Test3(val value1: String, val value2: String): NonExportedWithComponent1()