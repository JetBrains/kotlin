// ES_MODULES
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: lib
// FILE: lib.kt

@JsExport
val `my@invalid variable` = 42

// FILE: test.mjs
// ENTRY_ES_MODULE
import { "my@invalid variable" as anInvalidVariable } from "./topLevelExportedVariable-lib_v5.mjs";

export function box() {
    var variableValue = anInvalidVariable.get()

    if (variableValue !== 42)
        return "false: expect exproted 'my@invalid variable' to be 42 but it equals " + variableValue

    return "OK"
}