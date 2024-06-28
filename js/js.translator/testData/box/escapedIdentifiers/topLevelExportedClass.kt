// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: export_invalid_name_class
// FILE: lib.kt

@JsExport
class `invalid@class-name `() {
    fun foo(): Int = 42
}

// FILE: test.js
function box() {
    var InvalidClass = this["export_invalid_name_class"]["invalid@class-name "]
    var instance = new InvalidClass()
    var value = instance.foo()

    if (value !== 42)
        return "false: expect exproted class function to return 42 but it equals " + value

    return "OK"
}