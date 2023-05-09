// EXPECTED_REACHABLE_NODES: 1252
// IGNORE_BACKEND: JS
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE
// ES_MODULES

// KT-58246
// MODULE: lib
// FILE: lib.kt
@JsExport
var result = 0

@JsExport
class Test {
    init {
        result += 1
    }
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { result, Test } from "./exportClassWithInitBlock-lib_v5.mjs";

export function box() {
    new Test()

    if (result.get() != 1) return "fail: init block didn't call or called more than 1 time"

    return "OK"
}
