/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import test.concurrent.ThreadPool
import kotlin.test.Ignore

@OptIn(ExperimentalAtomicApi::class)
class CollectionsConcurrentAccessStressTest {
    @BeforeTest fun init() = ThreadPool.init(Platform.getAvailableProcessors())
    @AfterTest fun deinit() = ThreadPool.deinit()

    @Ignore // The test is slow, so run it only when needed
    @Test // regression test of KT-91299
    fun hashMapEntriesIteration() = stressTest { putAll(it) }

    @Ignore // The test is slow, so run it only when needed
    @Test // regression test of KT-91299
    fun hashMapKeysIteration() = stressTest { it.keys.forEach { put(it, it) } }

    @Ignore // The test is slow, so run it only when needed
    @Test // regression test of KT-91299
    fun hashMapValuesIteration() = stressTest { it.values.forEach { put(it, it) } }

    private inline fun stressTest(crossinline onMapUpdate: MutableMap<Int, Int>.(Map<Int, Int>) -> Unit) {
        val originalMap = IntArray(1) { it }.associateWith { it }
        val mapRef = AtomicReference(originalMap)

        val futures = ThreadPool.execute {
            repeat(100_000) {
                val _ = mapRef.updateAndFetch {
                    buildMap {
                        onMapUpdate(it)
                    }
                }
            }
        }
        futures.forEach { val _ = it.result }

        assertEquals(originalMap, mapRef.load())
    }
}
