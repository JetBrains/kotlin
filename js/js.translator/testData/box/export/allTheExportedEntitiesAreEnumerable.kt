// TARGET_BACKEND: JS_IR, JS_IR_ES6
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

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

// FILE: test.js
function box() {
    var allTheExportedEntities = Object.keys(this.lib.foo);

    if (allTheExportedEntities[0] !== 'valueSimpleProperty') return "Error: 'valueSimpleProperty' is not enumerable"
    if (allTheExportedEntities[1] !== 'variableSimpleProperty') return "Error: 'variableSimpleProperty' is not enumerable"
    if (allTheExportedEntities[2] !== 'valueGetterProperty') return "Error: 'valueGetterProperty' is not enumerable"
    if (allTheExportedEntities[3] !== 'variableGetterSetterProperty') return "Error: 'variableGetterSetterProperty' is not enumerable"
    if (allTheExportedEntities[4] !== 'simpleFunction') return "Error: 'simpleFunction' is not enumerable"
    if (allTheExportedEntities[5] !== 'SimpleClass') return "Error: 'SimpleClass' is not enumerable"
    if (allTheExportedEntities[6] !== 'SimpleObject') return "Error: 'SimpleObject' is not enumerable"
    if (allTheExportedEntities.length !== 7) return "Error: some extra entities are visible"

    return "OK"
}