// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC

// In optimized builds, the SLP vectorizer turns the four adjacent field loads below into vector loads
// that are still in use after the collection.

class Quad(var a: Any?, var b: Any?, var c: Any?, var d: Any?)
class Item(val id: Int)

fun sameAll(q: Quad, x: Any?, y: Any?, z: Any?, w: Any?): Boolean {
    val a = q.a; val b = q.b; val c = q.c; val d = q.d
    GC.collect()
    return (a === x) and (b === y) and (c === z) and (d === w)
}

// The loaded values are the only references to the items during the collection.
fun takeAll(q: Quad, wa: WeakReference<Any>, wb: WeakReference<Any>, wc: WeakReference<Any>, wd: WeakReference<Any>): Boolean {
    val a = q.a; val b = q.b; val c = q.c; val d = q.d
    q.a = null; q.b = null; q.c = null; q.d = null
    GC.collect()
    return (wa.value === a) and (wb.value === b) and (wc.value === c) and (wd.value === d)
}

fun makeQuad() = Quad(Item(0), Item(1), Item(2), Item(3))

fun box(): String {
    val shared = makeQuad()
    if (!sameAll(shared, shared.a, shared.b, shared.c, shared.d)) return "FAIL sameAll"

    val q = makeQuad()
    val wa = WeakReference(q.a!!)
    val wb = WeakReference(q.b!!)
    val wc = WeakReference(q.c!!)
    val wd = WeakReference(q.d!!)
    if (!takeAll(q, wa, wb, wc, wd)) return "FAIL takeAll"
    return "OK"
}
