// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi,kotlinx.cinterop.ExperimentalForeignApi,kotlin.native.runtime.NativeRuntimeApi,kotlin.native.internal.InternalForKotlinNative

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.identityHashCode
import kotlin.native.internal.isPermanent
import kotlin.native.internal.ref.*
import kotlin.native.runtime.GC
import kotlin.native.NoInline
import kotlin.test.*

typealias Id = Int
typealias Ref = Pair<Id, ExternalRCRef>

@NoInline
fun createLocal() : Ref {
    val obj = Any()
    return obj.identityHashCode() to createRetainedExternalRCRef(obj)
}

val globalObj = Any()

fun createGlobal() : Ref {
    return globalObj.identityHashCode() to createRetainedExternalRCRef(globalObj)
}

object Singleton {
    val x = Any()
}

fun createSingleton() : Ref {
    assertFalse(Singleton.isPermanent())
    return Singleton.identityHashCode() to createRetainedExternalRCRef(Singleton)
}

object Permanent

fun createPermanent() : Ref {
    assertTrue(Permanent.isPermanent())
    return Permanent.identityHashCode() to createRetainedExternalRCRef(Permanent)
}

inline fun createAndDisposeTest(create: () -> Ref) {
    val ref = create()
    GC.collect()
    assertEquals(ref.first, dereferenceExternalRCRef(ref.second).identityHashCode())
    retainExternalRCRef(ref.second)
    releaseExternalRCRef(ref.second)
    releaseExternalRCRef(ref.second)
    disposeExternalRCRef(ref.second)
}

@Test fun createAndDisposeLocal() {
    createAndDisposeTest(::createLocal)
}

@Test fun createAndDisposeGlobal() {
    createAndDisposeTest(::createGlobal)
}

@Test fun createAndDisposeSingleton() {
    createAndDisposeTest(::createSingleton)
}

@Test fun createAndDisposePermanent() {
    createAndDisposeTest(::createPermanent)
}

inline fun weakTestSuccess(create: () -> Ref) {
    val ref = create()
    releaseExternalRCRef(ref.second)
    GC.collect()
    assertFalse(tryRetainExternalRCRef(ref.second))
    disposeExternalRCRef(ref.second)
}

inline fun weakTestFailure(create: () -> Ref) {
    val ref = create()
    releaseExternalRCRef(ref.second)
    GC.collect()
    assertTrue(tryRetainExternalRCRef(ref.second))
    assertEquals(ref.first, dereferenceExternalRCRef(ref.second).identityHashCode())
    releaseExternalRCRef(ref.second)
    disposeExternalRCRef(ref.second)
}

@Test fun weakLocal() {
    weakTestSuccess(::createLocal)
}

@Test fun weakGlobal() {
    weakTestFailure(::createGlobal)
}

@Test fun weakSingleton() {
    weakTestFailure(::createSingleton)
}

@Test fun weakPermanent() {
    weakTestFailure(::createPermanent)
}

inline fun weakResurrectTest(create: () -> Ref) {
    val ref = create()
    val state = AtomicInt(0)
    withWorker {
        releaseExternalRCRef(ref.second)
        val future = execute(TransferMode.SAFE, { ref to state }) { (ref, state) ->
            state.value = 1
            while (state.value == 1) {
                val success = tryRetainExternalRCRef(ref.second)
                if (success) {
                    assertEquals(ref.first, dereferenceExternalRCRef(ref.second).identityHashCode())
                    releaseExternalRCRef(ref.second)
                }
            }
            Unit
        }
        while (state.value == 0) {}
        repeat(10) {
            GC.collect()
        }
        state.value = 2
        future.result
    }
    disposeExternalRCRef(ref.second)
}

@Test fun weakResurrectLocal() {
    weakResurrectTest(::createLocal)
}

@Test fun weakResurrectGlobal() {
    weakResurrectTest(::createGlobal)
}

@Test fun weakResurrectSingleton() {
    weakResurrectTest(::createSingleton)
}

@Test fun weakResurrectPermanent() {
    weakResurrectTest(::createPermanent)
}
