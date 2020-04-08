/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.jps.incremental.CacheStatus
import org.jetbrains.kotlin.jps.incremental.loadDiff
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class CompositeLookupsCacheAttributesManagerTest {
    val manager = CompositeLookupsCacheAttributesManager(File("not-used"), setOf())

    @Test
    fun testNothingToJava() {
        assertEquals(
            CacheStatus.INVALID,
            manager.loadDiff(
                actual = null,
                expected = CompositeLookupsCacheAttributes(1, setOf("jvm"))
            ).status
        )
    }

    @Test
    fun testNothingToJavaAndJs() {
        assertEquals(
            CacheStatus.INVALID,
            manager.loadDiff(
                actual = null,
                expected = CompositeLookupsCacheAttributes(1, setOf("jvm", "js"))
            ).status
        )
    }

    @Test
    fun testJsToJava() {
        assertEquals(
            CacheStatus.INVALID,
            manager.loadDiff(
                actual = CompositeLookupsCacheAttributes(1, setOf("jvm")),
                expected = CompositeLookupsCacheAttributes(1, setOf("js"))
            ).status
        )
    }

    @Test
    fun testJsAndJavaToJava() {
        assertEquals(
            CacheStatus.VALID,
            manager.loadDiff(
                actual = CompositeLookupsCacheAttributes(1, setOf("jvm", "js")),
                expected = CompositeLookupsCacheAttributes(1, setOf("jvm"))
            ).status
        )
    }

    @Test
    fun testJsAndJavaToJavaWithOtherVersion() {
        assertEquals(
            CacheStatus.INVALID,
            manager.loadDiff(
                actual = CompositeLookupsCacheAttributes(1, setOf("jvm", "js")),
                expected = CompositeLookupsCacheAttributes(2, setOf("jvm"))
            ).status
        )
    }

    @Test
    fun testJavaToJsAndJava() {
        assertEquals(
            CacheStatus.INVALID,
            manager.loadDiff(
                actual = CompositeLookupsCacheAttributes(1, setOf("jvm")),
                expected = CompositeLookupsCacheAttributes(1, setOf("jvm", "js"))
            ).status
        )
    }
}