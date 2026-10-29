// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-90587

// MODULE: lib
// FILE: lib.kt
package lib

@JsExport
abstract class Base {
    abstract suspend fun value(suffix: String = "!"): String
}

// MODULE: main(lib)
// FILE: main.kt
import lib.Base

private class Implementation : Base() {
    override suspend fun value(suffix: String): String = "OK$suffix"
}

@JsExport
fun createValue(): Base = Implementation()

@JsExport
suspend fun readValue(value: Base): String = value.value()

// FILE: main.js
async function box() {
    const value = this.main.createValue();
    if (await value.value() !== "OK!") return "omitted argument";
    if (await value.value("?") !== "OK?") return "explicit argument";
    if (await this.main.readValue(value) !== "OK!") return "Kotlin base dispatch";
    return "OK";
}
