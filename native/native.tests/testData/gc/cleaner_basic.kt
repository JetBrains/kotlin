// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative,kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi,kotlinx.cinterop.ExperimentalForeignApi

import kotlin.test.*

import kotlin.native.internal.*
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicNativePtr
import kotlin.native.concurrent.*
import kotlin.native.ref.WeakReference
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner
import kotlin.native.runtime.GC

class AtomicBoolean(initialValue: Boolean) {
    private val impl = AtomicInt(if (initialValue) 1 else 0)

    public var value: Boolean
        get() = impl.value != 0
        set(new) { impl.value = if (new) 1 else 0 }
}

class FunBox(private val impl: () -> Unit) {
    fun call() {
        impl()
    }
}

@Test
fun testCleanerLambda() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox) { it.call() }
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    GC.collect() // free funBox

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

@Test
fun testCleanerAnonymousFunction() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox, fun (it: FunBox) { it.call() })
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    GC.collect() // free funBox

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

@Test
fun testCleanerFunctionReference() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox, FunBox::call)
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    GC.collect() // free funBox

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

val globalInt = AtomicInt(0)

@Test
fun testCleanerWithInt() {
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = createCleaner(42) {
            globalInt.value = it
        }
        cleanerWeak = WeakReference(cleaner)
        assertEquals(0, globalInt.value)
    }()

    GC.collect()

    assertNull(cleanerWeak!!.value)
    assertEquals(42, globalInt.value)
}

val globalPtr = AtomicNativePtr(NativePtr.NULL)

@Test
fun testCleanerWithNativePtr() {
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = createCleaner(NativePtr.NULL + 42L) {
            globalPtr.value = it
        }
        cleanerWeak = WeakReference(cleaner)
        assertEquals(NativePtr.NULL, globalPtr.value)
    }()

    GC.collect()

    assertNull(cleanerWeak!!.value)
    assertEquals(NativePtr.NULL + 42L, globalPtr.value)
}

@ThreadLocal
var tlsValue = 1

@Test
fun testCleanerWithTLS() {
    tlsValue = 2
    val result = AtomicInt(0)
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = createCleaner(result) {
            it.value = tlsValue
        }
        cleanerWeak = WeakReference(cleaner)
    }()

    GC.collect()

    assertNull(cleanerWeak!!.value)
    assertEquals(2, tlsValue)
    assertEquals(1, result.value)
}

@Test
fun testCleanerWithException() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val funBox = FunBox { called.value = true }
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) {
            it.call()
            error("Cleaner block failed")
        }
        cleanerWeak = WeakReference(cleaner)
    }()

    GC.collect()
    GC.collect() // free funBox

    assertNull(cleanerWeak!!.value)
    // Cleaners block started executing.
    assertTrue(called.value)
    // Even though the block failed, the captured funBox is freed.
    assertNull(funBoxWeak!!.value)
}

@ThreadLocal
var tlsCleaner: Cleaner? = null

@Test
fun testCleanerInTLS() {
    val worker = Worker.start()
    val result = AtomicInt(0)

    worker.execute(TransferMode.SAFE, {result}) { result ->
        tlsCleaner = createCleaner(result) {
            it.value = 1
        }
    }

    worker.requestTermination().result
    waitWorkerTermination(worker)
    GC.collect()

    assertEquals(1, result.value)
}