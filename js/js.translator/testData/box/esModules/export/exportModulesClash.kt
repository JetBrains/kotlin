// ES_MODULES
// SPLIT_PER_MODULE

// MODULE: child1
// FILE: lib1.kt

package a

@JsExport
fun example(): String = "child1"

// MODULE: child2
// FILE: lib2.kt

package a

@JsExport
fun example(): String = "child2"

// MODULE: parent(child1, child2)
// FILE: lib3.kt

import a.example

@JsExport
fun usage(): String {
    return example()
}

// FILE: entry.mjs
// ENTRY_ES_MODULE
import { usage } from "./exportModulesClash-parent_v5.mjs";

export function box() {
    return  usage()
}