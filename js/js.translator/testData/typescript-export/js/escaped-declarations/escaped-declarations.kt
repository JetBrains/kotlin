// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: escaped-interfaces.kt


package foo

@JsExport
fun `invalid@name sum`(x: Int, y: Int): Int =
    x + y

@JsExport
fun invalid_args_name_sum(`first value`: Int, `second value`: Int): Int =
    `first value` + `second value`

// Properties

@JsExport
val `invalid name val`: Int = 1
@JsExport
var `invalid@name var`: Int = 1

// Classes

@JsExport
class `Invalid A`
@JsExport
class A1(val `first value`: Int, var `second.value`: Int)
@JsExport
class A2 {
    var `invalid:name`: Int = 42
}
@JsExport
class A3 {
    fun `invalid@name sum`(x: Int, y: Int): Int =
        x + y

    fun invalid_args_name_sum(`first value`: Int, `second value`: Int): Int =
        `first value` + `second value`
}

@JsExport
class A4 {
    companion object {
        var `@invalid+name@` = 23
        fun `^)run.something.weird^(`(): String = ")_("
    }
}