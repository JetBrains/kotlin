/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import test.concurrent.ThreadPool
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalAtomicApi::class)
class CollectionsConcurrentAccessStressTest {
    @BeforeTest fun init() = ThreadPool.init(20)
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Test // regression test of KT-91299
    fun hashMapIteration() {
        val originalMap = IntArray(1) { it }.associate { it to it }
        val mapRef = AtomicReference(originalMap)

        val features = ThreadPool.execute {
            repeat(100_000) {
                val _ = mapRef.updateAndFetch {
                    buildMap {
                        putAll(it)
                    }
                }
            }
        }
        features.forEach { val _ = it.result }

        assertEquals(originalMap, mapRef.load())
    }
}
