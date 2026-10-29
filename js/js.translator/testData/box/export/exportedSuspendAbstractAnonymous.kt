// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-90587

// MODULE: lib
// FILE: lib.kt

@JsExport
abstract class Base {
    abstract suspend fun value(x: Int): String
}

private class NamedImplementation : Base() {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
fun anonymous(): Base = object : Base() {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
fun named(): Base = NamedImplementation()

@JsExport
suspend fun callBase(base: Base): String = base.value(42)

// FILE: main.js
async function box() {
    const anonymous = this.lib.anonymous();
    const named = this.lib.named();
    const results = [
        await anonymous.value(42),
        await named.value(42),
        await this.lib.callBase(anonymous),
        await this.lib.callBase(named)
    ].join(";");
    return results === "OK42;OK42;OK42;OK42" ? "OK" : results;
}
