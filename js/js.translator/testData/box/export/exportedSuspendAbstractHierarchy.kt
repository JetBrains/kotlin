// RUN_PLAIN_BOX_FUNCTION
// ISSUE: KT-90587

// MODULE: lib
// FILE: lib.kt

@JsExport
abstract class Base {
    abstract suspend fun value(x: Int = 7): String
}

@JsExport
abstract class Middle : Base() {
    abstract override suspend fun value(x: Int): String
}

open class HiddenImplementation : Middle() {
    override suspend fun value(x: Int): String = "OK$x"
}

@JsExport
class InheritedLeaf : HiddenImplementation()

@JsExport
class OverridingLeaf : HiddenImplementation() {
    override suspend fun value(x: Int): String = "LAST$x"
}

@JsExport
suspend fun callBase(base: Base): String = base.value() + "/" + base.value(2)

// FILE: main.js
async function box() {
    const inherited = new this.lib.InheritedLeaf();
    const overriding = new this.lib.OverridingLeaf();
    const results = [
        await inherited.value(),
        await inherited.value(2),
        await overriding.value(),
        await overriding.value(2),
        await this.lib.callBase(inherited),
        await this.lib.callBase(overriding)
    ].join(";");
    return results === "OK7;OK2;LAST7;LAST2;OK7/OK2;LAST7/LAST2" ? "OK" : results;
}
