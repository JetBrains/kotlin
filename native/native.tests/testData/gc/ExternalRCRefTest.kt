// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi,kotlinx.cinterop.ExperimentalForeignApi,kotlin.native.runtime.NativeRuntimeApi,kotlin.native.internal.InternalForKotlinNative

import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.identityHashCode
import kotlin.native.internal.NativePtr
import kotlin.native.internal.isPermanent
import kotlin.native.internal.ref.*
import kotlin.native.runtime.GC
import kotlin.test.*

typealias Id = Int
typealias Ref = Pair<Id, ExternalRCRef>

fun createLocal() : Ref {
    val obj = Any()
    return obj.identityHashCode() to createRetainedExternalRCRef(obj)
}

fun createLocalUnretained() : Ref {
    val obj = Any()
    return obj.identityHashCode() to createUnretainedExternalRCRef(obj)
}

val globalObj = Any()

fun createGlobal() : Ref {
    return globalObj.identityHashCode() to createRetainedExternalRCRef(globalObj)
}

fun createGlobalUnretained() : Ref {
    return globalObj.identityHashCode() to createUnretainedExternalRCRef(globalObj)
}

object Singleton {
    val x = Any()
}

fun createSingleton() : Ref {
    assertFalse(Singleton.isPermanent())
    return Singleton.identityHashCode() to createRetainedExternalRCRef(Singleton)
}

fun createSingletonUnretained() : Ref {
    assertFalse(Singleton.isPermanent())
    return Singleton.identityHashCode() to createUnretainedExternalRCRef(Singleton)
}

object Permanent

fun createPermanent() : Ref {
    assertTrue(Permanent.isPermanent())
    return Permanent.identityHashCode() to createRetainedExternalRCRef(Permanent)
}

fun createPermanentUnretained() : Ref {
    assertTrue(Permanent.isPermanent())
    return Permanent.identityHashCode() to createUnretainedExternalRCRef(Permanent)
}

fun createNull(): Ref {
    return null.identityHashCode() to createRetainedExternalRCRef(null)
}

fun createNullUnretained(): Ref {
    return null.identityHashCode() to createUnretainedExternalRCRef(null)
}

@Test fun nullExternalRCRef() {
    assertTrue(createRetainedExternalRCRef(null) == NativePtr.NULL)
    assertTrue(createUnretainedExternalRCRef(null) == NativePtr.NULL)
    assertNull(dereferenceExternalRCRef(NativePtr.NULL))
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

@Test fun createAndDisposeNull() {
    createAndDisposeTest(::createNull)
}

inline fun weakTestSuccess(create: () -> Ref) {
    val ref = create()
    GC.collect()
    assertNull(dereferenceExternalRCRefOrNull(ref.second))
    disposeExternalRCRef(ref.second)
}

inline fun weakTestFailure(create: () -> Ref) {
    val ref = create()
    GC.collect()
    assertEquals(ref.first, dereferenceExternalRCRefOrNull(ref.second).identityHashCode())
    assertEquals(ref.first, dereferenceExternalRCRef(ref.second).identityHashCode())
    disposeExternalRCRef(ref.second)
}

@Test fun weakLocal() {
    weakTestSuccess(::createLocalUnretained)
}

@Test fun weakGlobal() {
    weakTestFailure(::createGlobalUnretained)
}

@Test fun weakSingleton() {
    weakTestFailure(::createSingletonUnretained)
}

@Test fun weakPermanent() {
    weakTestFailure(::createPermanentUnretained)
}

@Test fun weakNull() {
    weakTestSuccess(::createNullUnretained)
}

inline fun weakResurrectTest(create: () -> Ref, doRetainRelease: Boolean) {
    val ref = create()
    val state = AtomicInt(0)
    withWorker {
        releaseExternalRCRef(ref.second)
        val future = execute(TransferMode.SAFE, { Triple(ref, state, doRetainRelease) }) { (ref, state, doRetainRelease) ->
            state.value = 1
            while (state.value == 1) {
                val result = dereferenceExternalRCRefOrNull(ref.second)
                if (result != null) {
                    if (doRetainRelease) retainExternalRCRef(ref.second)
                    assertEquals(ref.first, result.identityHashCode())
                    assertEquals(ref.first, dereferenceExternalRCRef(ref.second).identityHashCode())
                    if (doRetainRelease) releaseExternalRCRef(ref.second)
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
    weakResurrectTest(::createLocal, false)
}

@Test fun weakResurrectGlobal() {
    weakResurrectTest(::createGlobal, false)
}

@Test fun weakResurrectSingleton() {
    weakResurrectTest(::createSingleton, false)
}

@Test fun weakResurrectPermanent() {
    weakResurrectTest(::createPermanent, false)
}

@Test fun weakResurrectNull() {
    weakResurrectTest(::createNull, false)
}

@Test fun weakResurrectLocalWithRetainRelease() {
    weakResurrectTest(::createLocal, true)
}

@Test fun weakResurrectGlobalWithRetainRelease() {
    weakResurrectTest(::createGlobal, true)
}

@Test fun weakResurrectSingletonWithRetainRelease() {
    weakResurrectTest(::createSingleton, true)
}

@Test fun weakResurrectPermanentWithRetainRelease() {
    weakResurrectTest(::createPermanent, true)
}

@Test fun weakResurrectNullWithRetainRelease() {
    weakResurrectTest(::createNull, true)
}
