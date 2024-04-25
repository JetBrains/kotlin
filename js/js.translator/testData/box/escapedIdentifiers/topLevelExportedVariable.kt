// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: export_invalid_name_variable
// FILE: lib.kt

@JsExport
val `my@invalid variable` = 42

// FILE: test.js
function box() {
    var variableValue = this["export_invalid_name_variable"]["my@invalid variable"]

    if (variableValue !== 42)
        return "false: expect exproted 'my@invalid variable' to be 42 but it equals " + variableValue

    return "OK"
}