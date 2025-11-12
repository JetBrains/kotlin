// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: enum-classes.kt

@file:JsExport

package foo


enum class Uninhabited


enum class TestEnumClass(val constructorParameter: String) {
    A("aConstructorParameter"),
    B("bConstructorParameter"),
    @JsName("CustomNamedEntry") C("cConstructorParameter");

    val foo = ordinal

    fun bar(value: String) = value

    fun bay() = name

    class Nested {
        val prop: String = "hello2"
    }
}


class OuterClass {
    enum class NestedEnum {
        A,
        B
    }
}
