// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// EXPORT_UNTYPED_AS_UNKNOWN
// MODULE: JS_TESTS
// WITH_STDLIB
// FILE: export-untyped-as-unknown.kt

package foo

@JsExport
val _any: Any = Any()

@JsExport
val _nullable_any: Any? = null

@JsExport
val _array_any: Array<Any> = emptyArray()

@JsExport
fun consumeAny(value: Any): Any = value

@JsExport
fun consumeNullableAny(value: Any?): Any? = value

@JsExport
fun produceDynamic(): dynamic = 42

@JsExport
fun consumeDynamic(value: dynamic): dynamic = value

@JsExport
fun consumeNullableDynamic(value: dynamic?): dynamic? = value

@JsExport
class WithDynamicMembers {
    val anyProperty: Any = Any()
    val dynamicProperty: dynamic = 42
    fun anyMethod(value: Any): Any = value
    fun dynamicMethod(value: dynamic): dynamic = value
}
