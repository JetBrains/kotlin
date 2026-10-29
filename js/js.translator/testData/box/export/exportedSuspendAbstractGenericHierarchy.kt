// RUN_PLAIN_BOX_FUNCTION
// KT-91641
// Move to typescript-export/js after KT-91641 is fixed.

// MODULE: lib
// FILE: lib.kt

@JsExport
abstract class Base<T> {
    abstract suspend fun value(x: T): T
}

@JsExport
abstract class Middle : Base<Int>() {
    abstract override suspend fun value(x: Int): Int
}

open class HiddenImplementation : Middle() {
    override suspend fun value(x: Int): Int = x + 1
}

@JsExport
class InheritedLeaf : HiddenImplementation()

@JsExport
class OverridingLeaf : HiddenImplementation() {
    override suspend fun value(x: Int): Int = x + 2
}

@JsExport
suspend fun callBase(base: Base<Int>, x: Int): Int = base.value(x)

@JsExport
suspend fun callMiddle(middle: Middle, x: Int): Int = middle.value(x)

// FILE: main.js
async function box() {
    const lib = this.lib;
    const inherited = new lib.InheritedLeaf();
    const overriding = new lib.OverridingLeaf();
    const results = [
        await inherited.value(7),
        await inherited.value(2),
        await lib.callBase(inherited, 42),
        await lib.callMiddle(inherited, 42),
        await overriding.value(7),
        await overriding.value(2),
        await lib.callBase(overriding, 42),
        await lib.callMiddle(overriding, 42)
    ].join(";");
    return results === "8;3;43;43;9;4;44;44" ? "OK" : results;
}
