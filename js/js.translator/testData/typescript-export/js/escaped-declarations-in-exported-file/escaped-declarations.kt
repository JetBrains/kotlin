// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: escaped-interfaces.kt

@file:JsExport


package foo


fun `invalid@name sum`(x: Int, y: Int): Int =
    x + y


fun invalid_args_name_sum(`first value`: Int, `second value`: Int): Int =
    `first value` + `second value`

// Properties


val `invalid name val`: Int = 1

var `invalid@name var`: Int = 1

// Classes


class `Invalid A`

class A1(val `first value`: Int, var `second.value`: Int)

class A2 {
    var `invalid:name`: Int = 42
}

class A3 {
    fun `invalid@name sum`(x: Int, y: Int): Int =
        x + y

    fun invalid_args_name_sum(`first value`: Int, `second value`: Int): Int =
        `first value` + `second value`
}


class A4 {
    companion object {
        var `@invalid+name@` = 23
        fun `^)run.something.weird^(`(): String = ")_("
    }
}