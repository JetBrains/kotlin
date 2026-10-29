// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// MODULE: lib
// FILE: lib.kt
package lib

@JsExport
abstract class Base {
    abstract suspend fun value(suffix: String = "!"): String
}

// MODULE: main(lib)
// FILE: abstract-suspend-cross-module.kt
package foo

import lib.Base

private class Implementation : Base() {
    override suspend fun value(suffix: String): String = "OK$suffix"
}

@JsExport
fun createValue(): Base = Implementation()

@JsExport
suspend fun readValue(value: Base): String = value.value()
