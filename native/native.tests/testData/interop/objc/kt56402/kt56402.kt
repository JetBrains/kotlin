/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kt56402.*

import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.native.concurrent.*
import kotlin.native.internal.test.testLauncherEntryPoint
import kotlin.system.exitProcess
import kotlin.test.*
import kotlinx.cinterop.*

class LockedSet<T> {
    private val lock = AtomicInt(0)
    private val impl = mutableSetOf<T>()

    private inline fun <R> locked(f: () -> R): R {
        while (!lock.compareAndSet(0, 1)) {}
        try {
            return f()
        } finally {
            lock.value = 0
        }
    }

    fun add(id: T) = locked {
        assertFalse(id in impl)
        impl.add(id)
    }

    fun remove(id: T) = locked {
        assertTrue(id in impl)
        impl.remove(id)
    }

    operator fun contains(id: T) = locked {
        id in impl
    }
}

class OnDestroyHookSub(onDestroy: (ULong) -> Unit) : OnDestroyHook(onDestroy)

val aliveObjectIds = LockedSet<ULong>()

// Keeps the last allocated object strongly reachable until it is registered in `aliveObjectIds`.
// Otherwise the GC may collect it and run `onDestroy` (which removes the id from the set)
// on the finalizer thread before `add` happens, failing the assertion in `LockedSet.remove`.
val keepAlive = AtomicReference<Any?>(null)

@NoInline fun alloc(ctor: ((ULong) -> Unit) -> Pair<Any, ULong>): ULong = autoreleasepool {
    val (obj, id) = ctor {
        aliveObjectIds.remove(it)
    }
    keepAlive.value = obj
    aliveObjectIds.add(id)
    keepAlive.value = null
    id
}

fun waitDestruction(id: ULong) {
    assertTrue(isMainThread())
    // Make sure the finalizers are not run on the main thread even for STMS.
    withWorker {
        execute(TransferMode.SAFE, {}) {
            kotlin.native.runtime.GC.collect()
        }.result
    }
    while (true) {
        spin()
        if (!(id in aliveObjectIds)) {
            break
        }
    }
}

@Test
fun testOnMainThread() {
    assertTrue(isMainThread())
    val id = alloc { onDestroy ->
        val obj = OnDestroyHook {
            assertTrue(isMainThread())
            onDestroy(it)
        }
        obj to obj.identity()
    }
    waitDestruction(id)
}

@Test
fun testOnSecondaryThread() {
    val id = withWorker {
        execute(TransferMode.SAFE, {}) {
            assertFalse(isMainThread())
            alloc { onDestroy ->
                val obj = OnDestroyHook {
                    assertFalse(isMainThread())
                    onDestroy(it)
                }
                obj to obj.identity()
            }
        }.result
    }
    waitDestruction(id)
}

@Test
fun testSubOnMainThread() {
    assertTrue(isMainThread())
    val id = alloc { onDestroy ->
        val obj = OnDestroyHookSub {
            assertTrue(isMainThread())
            onDestroy(it)
        }
        obj to obj.identity()
    }
    waitDestruction(id)
}

@Test
fun testSubOnSecondaryThread() {
    val id = withWorker {
        execute(TransferMode.SAFE, {}) {
            assertFalse(isMainThread())
            alloc { onDestroy ->
                val obj = OnDestroyHookSub {
                    assertFalse(isMainThread())
                    onDestroy(it)
                }
                obj to obj.identity()
            }
        }.result
    }
    waitDestruction(id)
}

@Test
fun testGlobalOnMainThread() {
    assertTrue(isMainThread())
    val id = alloc { onDestroy ->
        val obj = newGlobal {
            assertTrue(isMainThread())
            onDestroy(it)
        }!!
        clearGlobal()
        obj to obj.identity()
    }
    waitDestruction(id)
}

@Test
fun testGlobalOnSecondaryThread() {
    val id = withWorker {
        execute(TransferMode.SAFE, {}) {
            assertFalse(isMainThread())
            alloc { onDestroy ->
                val obj = newGlobal {
                    assertFalse(isMainThread())
                    onDestroy(it)
                }!!
                clearGlobal()
                obj to obj.identity()
            }
        }.result
    }
    waitDestruction(id)
}

@Test
fun testProtocolOnMainThread() {
    assertTrue(isMainThread())
    val id = alloc { onDestroy ->
        val obj = newOnDestroyHook {
            assertTrue(isMainThread())
            onDestroy(it)
        }!!
        obj to obj.identity()
    }
    waitDestruction(id)
}

@Test
fun testProtocolOnSecondaryThread() {
    val id = withWorker {
        execute(TransferMode.SAFE, {}) {
            assertFalse(isMainThread())
            alloc { onDestroy ->
                val obj = newOnDestroyHook {
                    assertFalse(isMainThread())
                    onDestroy(it)
                }!!
                obj to obj.identity()
            }
        }.result
    }
    waitDestruction(id)
}

fun main(args: Array<String>) = startApp {
    val exitCode = testLauncherEntryPoint(args)
    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}
