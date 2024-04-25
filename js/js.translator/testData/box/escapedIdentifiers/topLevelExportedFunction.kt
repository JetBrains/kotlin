// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: export_invalid_name_function
// FILE: lib.kt

@JsExport
fun `@do something like-this`(`test value`: Int = 42): Int = `test value`

// FILE: test.js
function box() {
    var value = this["export_invalid_name_function"]["@do something like-this"]()

    if (value !== 42)
        return "false: expect exproted function '@do something like-this' to return 42 but it equals " + value

    return "OK"
}