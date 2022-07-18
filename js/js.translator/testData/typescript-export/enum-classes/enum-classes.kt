// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: enum-classes.kt

package foo

@JsExport
enum class TestEnumClass(val constructorParameter: String) {
    A("aConstructorParameter"),
    B("bConstructorParameter");

    val foo = ordinal

    fun bar(value: String) = value

    fun bay() = name

    class Nested {
        val prop: String = "hello2"
    }
}

@JsExport
class OuterClass {
    enum class NestedEnum {
        A,
        B
    }
}

