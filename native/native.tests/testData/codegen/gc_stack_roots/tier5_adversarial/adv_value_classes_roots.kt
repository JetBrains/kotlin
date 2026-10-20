// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC
import kotlin.native.ref.WeakReference

class HeavyPayload(var text: String)

value class RefBox(val payload: HeavyPayload)

value class PrimBox(val counter: Long)

value class NullableRefBox(val payload: HeavyPayload?)

fun testUnboxedRefValueClass(): String {
    val payload = HeavyPayload("payload_1")
    val weakPayload = WeakReference(payload)
    val refBox = RefBox(payload)

    GC.collect()

    if (weakPayload.value == null) return "FAIL: unboxed RefBox payload was collected"
    if (refBox.payload.text != "payload_1") return "FAIL: unboxed RefBox payload text corrupted"

    refBox.payload.text = "payload_mutated"
    GC.collect()

    if (refBox.payload.text != "payload_mutated") return "FAIL: mutated payload corrupted"
    return "OK"
}

fun <T> identityGeneric(item: T): T {
    GC.collect()
    return item
}

fun testBoxedValueClass(): String {
    val payload = HeavyPayload("payload_generic")
    val refBox = RefBox(payload)

    val returnedBox = identityGeneric(refBox)

    if (returnedBox.payload.text != "payload_generic") {
        return "FAIL: boxed value class generic roundtrip failed: ${returnedBox.payload.text}"
    }

    return "OK"
}

fun testPrimitiveValueClass(): String {
    val prim = PrimBox(0x123456789ABCDEF0L)
    GC.collect()
    if (prim.counter != 0x123456789ABCDEF0L) return "FAIL: PrimBox counter corrupted"
    return "OK"
}

fun testNullableRefValueClass(): String {
    val nullBox = NullableRefBox(null)
    GC.collect()
    if (nullBox.payload != null) return "FAIL: NullableRefBox(null) was not null"

    val nonNullBox = NullableRefBox(HeavyPayload("non_null"))
    GC.collect()
    if (nonNullBox.payload?.text != "non_null") return "FAIL: NullableRefBox text corrupted"
    return "OK"
}

fun box(): String {
    var res = testUnboxedRefValueClass()
    if (res != "OK") return res

    res = testBoxedValueClass()
    if (res != "OK") return res

    res = testPrimitiveValueClass()
    if (res != "OK") return res

    res = testNullableRefValueClass()
    if (res != "OK") return res

    return "OK"
}
