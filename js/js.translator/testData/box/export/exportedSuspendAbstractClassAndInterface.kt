// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-90587

// MODULE: lib
// FILE: lib.kt

@JsExport
abstract class Base {
    abstract suspend fun value(x: Int): String
}

@JsExport
interface ValueProvider {
    suspend fun value(x: Int): String
}

@JsExport
class Implementation : Base(), ValueProvider {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
suspend fun callBase(value: Base, x: Int): String = value.value(x)

@JsExport
suspend fun callInterface(value: ValueProvider, x: Int): String = value.value(x)

// FILE: main.js
async function box() {
    const lib = this.lib;
    const value = new lib.Implementation();
    const direct = await value.value(1);
    const viaBase = await lib.callBase(value, 2);
    const viaInterface = await lib.callInterface(value, 3);
    const result = direct + ";" + viaBase + ";" + viaInterface;
    return result === "OK1;OK2;OK3" ? "OK" : result;
}
