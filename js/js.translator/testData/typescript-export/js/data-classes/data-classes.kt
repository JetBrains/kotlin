// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// MODULE: JS_TESTS
// FILE: data-classes.kt

package foo


@JsExport
data class TestDataClass(val name: String) {
    class Nested {
        val prop: String = "hello"
    }
}

@JsExport
data class KT39423(
    val a: String,
    val b: Int? = null
)

@JsExport
abstract class WithComponent1 {
    abstract fun component1(): String
}

@JsExport.Ignore
abstract class NonExportedWithComponent1 {
    abstract fun component1(): String
}

@JsExport
data class Test2(val value1: String, val value2: String): WithComponent1()

@JsExport
data class Test3(val value1: String, val value2: String): NonExportedWithComponent1()

@JsExport
fun shortNameBasedDestructuring(): String {
    val (value2, value1) = Test2("4", "2")
    return value1 + value2
}

@JsExport
fun fullNameBasedDestructuring(): String {
    (val value2, var value1) = Test2("4", "2")
    value1 += " "
    return value1 + value2
}

@JsExport
fun shortPositionBasedDestructuring(): String {
    val [v1, v2] = Test2("4", "2")
    return v1 + v2
}

@JsExport
fun fullPositionBasedDestructuring(): String {
    [var v1, val v2] = Test2("4", "2")
    v1 += " "
    return v1 + v2
}