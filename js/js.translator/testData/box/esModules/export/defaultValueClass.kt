// ES_MODULES
// SPLIT_PER_MODULE

// MODULE: lib
// FILE: lib.kt
@JsExport.Default
value class DefaultValue(val value: String)

// FILE: entry.mjs
// ENTRY_ES_MODULE

import DefaultValue from "./defaultValueClass-lib_v5.mjs";

export function box() {
    const value = new DefaultValue("OK");
    return value.value;
}
