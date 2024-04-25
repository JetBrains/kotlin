// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: export_invalid_name_class
// FILE: lib.kt

@JsExport
class `invalid@class-name `() {
    companion object {
        val `@invalid val@`: Int = 23
        fun `invalid fun`(): Int = 42
    }
}

// FILE: test.js
function box() {
    var InvalidClass = this["export_invalid_name_class"]["invalid@class-name "]

    if (InvalidClass.Companion["@invalid val@"] !== 23)
        return "false: expect exproted class static variable '@invalid val@' to be 23 but it equals " + InvalidClass.Companion["@invalud val@"]

    var result = InvalidClass.Companion["invalid fun"]()

    if (result !== 42)
        return "false: expect exproted class static function 'invalid fun' to return 23 but it equals " + result

    return "OK"
}