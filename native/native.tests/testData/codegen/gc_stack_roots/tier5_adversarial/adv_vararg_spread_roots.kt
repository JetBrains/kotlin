// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class VarargItem(val key: String, val value: Int)

fun processVarargs(vararg items: VarargItem): Int {
    GC.collect()

    var sum = 0
    for (item in items) {
        sum += item.value
    }
    return sum
}

fun testVarargSpread(): String {
    val a = VarargItem("A", 10)
    val b = VarargItem("B", 20)
    val initialArray = arrayOf(a, b)

    val c = VarargItem("C", 30)
    val d = VarargItem("D", 40)

    val weakC = WeakReference(c)

    val total = processVarargs(*initialArray, c, d)

    GC.collect()

    if (total != 100) return "FAIL: vararg sum incorrect: $total"
    if (weakC.value == null) return "FAIL: vararg item C collected prematurely"
    if (c.key != "C") return "FAIL: vararg item C corrupted: ${c.key}"

    return "OK"
}

fun testEmptyVarargs(): String {
    val total = processVarargs()
    GC.collect()
    if (total != 0) return "FAIL: empty vararg returned non-zero: $total"
    return "OK"
}

fun box(): String {
    var res = testVarargSpread()
    if (res != "OK") return res

    res = testEmptyVarargs()
    if (res != "OK") return res

    return "OK"
}
