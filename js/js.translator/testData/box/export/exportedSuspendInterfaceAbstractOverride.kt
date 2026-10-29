// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-90925

// MODULE: lib
// FILE: lib.kt

@JsExport
interface ValueProvider {
    suspend fun value(x: Int = 7): String
}

@JsExport
abstract class Base : ValueProvider {
    abstract override suspend fun value(x: Int): String
}

@JsExport
class Implementation : Base() {
    override suspend fun value(x: Int): String = "K" + x
}

// FILE: main.js
async function box() {
    const value = new this.lib.Implementation();
    if (await value.value(2) !== "K2") return "explicit argument";
    if (await value.value() !== "K7") return "omitted argument";
    return "OK";
}
