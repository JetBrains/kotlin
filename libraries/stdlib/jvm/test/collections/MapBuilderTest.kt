/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package test.collections

import kotlin.collections.builders.MapBuilder
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.Test

class MapBuilderTest {

    @Test
    fun capacityOverflow() {
        val builderSize = 15
        val giantMapSize = Int.MAX_VALUE - builderSize + 1

        val giantMap = object : AbstractMap<Int, String>() {
            override val entries: Set<Map.Entry<Int, String>> = object : AbstractSet<Map.Entry<Int, String>>() {
                override val size: Int get() = giantMapSize
                override fun iterator(): Iterator<Map.Entry<Int, String>> {
                    return indexSequence().map {
                        object : Map.Entry<Int, String> {
                            override val key: Int get() = it
                            override val value: String get() = "value"
                        }
                    }.take(size).iterator()
                }
            }
        }

        buildMap {
            repeat(builderSize) { put(-it, "value") }

            assertFails { putAll(giantMap) }
            assertEquals(builderSize, size)
        }
    }

    // KT-53310
    @Test
    fun reclaimStorage() {
        val builder = MapBuilder<Int, Int>()
        val initialCapacity = builder.capacity
        repeat(20) {
            builder[it] = it
            builder.remove(it)
        }
        assertEquals(initialCapacity, builder.capacity)
    }
}