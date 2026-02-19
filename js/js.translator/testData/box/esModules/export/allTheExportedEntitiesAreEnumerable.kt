// ES_MODULES

// MODULE: lib
// FILE: lib.kt
@file:JsExport
package foo

val valueSimpleProperty = "foo"
var variableSimpleProperty = "foo"

val valueGetterProperty: String
    get() = "foo"

var variableGetterSetterProperty: String
    get() = variableSimpleProperty
    set(value) { variableSimpleProperty = value }

fun simpleFunction() {}

class SimpleClass {}

object SimpleObject

// FILE: test.mjs
// ENTRY_ES_MODULE
import * as lib from "./allTheExportedEntitiesAreEnumerable-lib_v5.mjs";

export function box() {
    var allTheExportedEntities = Object.keys(lib);

    if (allTheExportedEntities[0] !== 'SimpleClass') return "Error: 'SimpleClass' is not enumerable"
    if (allTheExportedEntities[1] !== 'SimpleObject') return "Error: 'SimpleObject' is not enumerable"
    if (allTheExportedEntities[2] !== 'simpleFunction') return "Error: 'simpleFunction' is not enumerable"
    if (allTheExportedEntities[3] !== 'valueGetterProperty') return "Error: 'valueGetterProperty' is not enumerable"
    if (allTheExportedEntities[4] !== 'valueSimpleProperty') return "Error: 'valueSimpleProperty' is not enumerable"
    if (allTheExportedEntities[5] !== 'variableGetterSetterProperty') return "Error: 'variableGetterSetterProperty' is not enumerable"
    if (allTheExportedEntities[6] !== 'variableSimpleProperty') return "Error: 'variableSimpleProperty' is not enumerable"
    if (allTheExportedEntities.length !== 7) return "Error: some extra entities are visible"

    return "OK"
}