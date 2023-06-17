/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, FreezingIsDeprecated::class)
package runtime.workers.lazy1

import kotlin.test.*
import kotlin.native.concurrent.*

private var y = 20

class Lazy(mode: LazyThreadSafetyMode) {
    val x = 17
    val self by lazy(mode) { this }
    val recursion: Int by lazy(mode) {
        if (x < 17) 42 else recursion
    }
    val finiteRecursion : Int by lazy(mode) {
        if (y < 17) 42 else {
            y -= 1
            finiteRecursion + 1
        }
    }
    val freezer: Int by lazy(mode) {
        freeze()
        42
    }
    val thrower: String by lazy(mode) {
        if (x < 100) throw IllegalArgumentException()
        "FAIL"
    }
}

private val checkedLazyModes =
        if (Platform.memoryModel != MemoryModel.EXPERIMENTAL)
            listOf(LazyThreadSafetyMode.PUBLICATION)
        else
            listOf(LazyThreadSafetyMode.SYNCHRONIZED, LazyThreadSafetyMode.PUBLICATION)

@Test fun runTest1() {
    for (mode in checkedLazyModes) {
        // We decided to synchonaize behaviour to be consistent with jvm version.
        // Anyway, it's doesn't looks like well-defined case
        if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
            val expected = if (mode == LazyThreadSafetyMode.SYNCHRONIZED) 46 else 42
            y = 20
            assertEquals(Lazy(mode).finiteRecursion, expected)
            y = 20
            assertEquals(Lazy(mode).freeze().finiteRecursion, expected)
        } else {
            assertFailsWith<IllegalStateException> {
                println(Lazy(mode).recursion)
            }
            assertFailsWith<IllegalStateException> {
                println(Lazy(mode).freeze().recursion)
            }
            y = 20
            assertFailsWith<IllegalStateException> {
                println(Lazy(mode).finiteRecursion)
            }
            y = 20
            assertFailsWith<IllegalStateException> {
                println(Lazy(mode).freeze().finiteRecursion)
            }
        }
    }
}

@Test fun runTest2() {
    for (mode in checkedLazyModes) {
        var sum = 0
        for (i in 1..100) {
            val self = Lazy(mode).freeze()
            assertEquals(self, self.self)
            sum += self.self.hashCode()
        }
    }
    println("OK")
}


@Test fun runTest3() {
    if (Platform.isFreezingEnabled) {
        for (mode in checkedLazyModes) {
            assertFailsWith<InvalidMutabilityException> {
                println(Lazy(mode).freezer)
            }
        }
    }
}

@Test fun runTest4() {
    for (mode in checkedLazyModes) {
        val self = Lazy(mode)
        repeat(10) {
            assertFailsWith<IllegalArgumentException> {
                println(self.thrower)
            }
        }
    }
}