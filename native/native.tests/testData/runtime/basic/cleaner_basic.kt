/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// Ideally, this test must fail with gcType=NOOP with any cache mode.
// KT-63944: unfortunately, GC flavours are silently not switched in presence of caches.
// As soon the issue would be fixed, please remove `&& cacheMode=NO` from next line.
// IGNORE_NATIVE: gcType=NOOP && cacheMode=NO

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, FreezingIsDeprecated::class,
        kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package runtime.basic.cleaner_basic

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

    init {
        freeze()
    }

    public var value: Boolean
        get() = impl.value != 0
        set(new) { impl.value = if (new) 1 else 0 }
}

class FunBox(private val impl: () -> Unit) {
    fun call() {
        impl()
    }
}

fun testCleanerLambda() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox) { it.call() }
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

fun testCleanerNonSharedLambda() {
    // Only for experimental MM.
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        return
    }
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
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

fun testCleanerAnonymousFunction() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox, fun (it: FunBox) { it.call() })
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

fun testCleanerNonSharedAnonymousFunction() {
    // Only for experimental MM.
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        return
    }
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
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

fun testCleanerFunctionReference() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox, FunBox::call)
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

fun testCleanerNonSharedFunctionReference() {
    // Only for experimental MM.
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        return
    }
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
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertTrue(called.value)
    assertNull(funBoxWeak!!.value)
}

fun testCleanerFailWithNonShareableArgument() {
    // Only for legacy MM.
    if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
        return
    }
    val funBox = FunBox {}
    assertFailsWith<IllegalArgumentException> {
        createCleaner(funBox) {}
    }
}

fun testCleanerCleansWithoutGC() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = {
            val funBox = FunBox { called.value = true }.freeze()
            funBoxWeak = WeakReference(funBox)
            createCleaner(funBox) { it.call() }
        }()
        GC.collect()  // Make sure local funBox reference is gone
        cleaner.freeze()
        cleanerWeak = WeakReference(cleaner)
        assertFalse(called.value)
    }()

    GC.collect()

    assertNull(cleanerWeak!!.value)

    waitCleanerWorker()

    assertTrue(called.value)

    // Only for legacy MM.
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        // If this fails, GC has somehow ran on the cleaners worker.
        assertNotNull(funBoxWeak!!.value)
    }
}

val globalInt = AtomicInt(0)

fun testCleanerWithInt() {
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val cleaner = createCleaner(42) {
            globalInt.value = it
        }.freeze()
        cleanerWeak = WeakReference(cleaner)
        assertEquals(0, globalInt.value)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertEquals(42, globalInt.value)
}

val globalPtr = AtomicNativePtr(NativePtr.NULL)

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
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    assertEquals(NativePtr.NULL + 42L, globalPtr.value)
}

fun testCleanerWithException() {
    val called = AtomicBoolean(false);
    var funBoxWeak: WeakReference<FunBox>? = null
    var cleanerWeak: WeakReference<Cleaner>? = null
    {
        val funBox = FunBox { called.value = true }.freeze()
        funBoxWeak = WeakReference(funBox)
        val cleaner = createCleaner(funBox) {
            it.call()
            error("Cleaner block failed")
        }
        cleanerWeak = WeakReference(cleaner)
    }()

    GC.collect()
    performGCOnCleanerWorker()

    assertNull(cleanerWeak!!.value)
    // Cleaners block started executing.
    assertTrue(called.value)
    // Even though the block failed, the captured funBox is freed.
    assertNull(funBoxWeak!!.value)
}

fun box(): String {
    testCleanerAnonymousFunction()
    testCleanerLambda()
    testCleanerCleansWithoutGC()
    testCleanerFunctionReference()
    testCleanerFailWithNonShareableArgument()
    testCleanerNonSharedAnonymousFunction()
    testCleanerNonSharedFunctionReference()
    testCleanerNonSharedLambda()
    testCleanerWithException()
    testCleanerWithInt()
    testCleanerWithNativePtr()

    return "OK"
}