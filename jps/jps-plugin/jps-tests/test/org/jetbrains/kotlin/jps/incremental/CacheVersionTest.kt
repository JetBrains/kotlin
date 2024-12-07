/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.load.kotlin.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheVersionTest {
    @Test
    fun testConstruct() {
        assertEquals(
            3011001,
            CacheVersion(
                3,
                JvmBytecodeBinaryVersion(1, 0, 3),
                MetadataVersion(1, 1, 13)
            ).intValue
        )
    }

    @Test
    fun testDeconstruct() {
        assertEquals(
            "CacheVersion(caches: 3, bytecode: 1.0, metadata: 1.1)",
            CacheVersion(3011001).toString()
        )
    }

    @Test
    fun testConstructDeconstruct() {
        val version = CacheVersion(
            1,
            JvmBytecodeBinaryVersion(2, 3),
            MetadataVersion(4, 5)
        )

        assertEquals(1024305, version.intValue)
        assertEquals("CacheVersion(caches: 1, bytecode: 2.3, metadata: 4.5)", version.toString())
    }

    @Test
    fun testMaxValues() {
        assertEquals(
            "CacheVersion(caches: 2146, bytecode: 9.9, metadata: 9.99)",
            CacheVersion(
                2146,
                JvmBytecodeBinaryVersion(9, 9),
                MetadataVersion(9, 99)
            ).toString()
        )
    }
}
