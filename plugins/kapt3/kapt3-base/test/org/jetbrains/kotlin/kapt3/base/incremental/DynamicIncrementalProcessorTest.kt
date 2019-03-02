/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test.org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.kapt3.base.incremental.RuntimeProcType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File


class DynamicIncrementalProcessorTest {
    @JvmField
    @Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private lateinit var generatedSources: File

    @Before
    fun setUp() {
        generatedSources = tmp.newFolder()
    }

    @Test
    fun testIfIsolating() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val dynamic = DynamicProcessor(kind = RuntimeProcType.ISOLATING).toDynamic()
        runAnnotationProcessing(srcFiles, listOf(dynamic), generatedSources)

        assertEquals(RuntimeProcType.ISOLATING, dynamic.getRuntimeType())

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/User.java").absoluteFile,
                generatedSources.resolve("test/AddressGenerated.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/Address.java").absoluteFile
            ),
            dynamic.getGeneratedToSources()
        )
    }

    @Test
    fun testIfAggregating() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val dynamic = DynamicProcessor(kind = RuntimeProcType.AGGREGATING).toDynamic()
        runAnnotationProcessing(srcFiles, listOf(dynamic), generatedSources)

        assertEquals(RuntimeProcType.AGGREGATING, dynamic.getRuntimeType())

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to null,
                generatedSources.resolve("test/AddressGenerated.java") to null
            ),
            dynamic.getGeneratedToSources()
        )
    }

    @Test
    fun testIfNonIncremental() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val dynamic = DynamicProcessor(kind = RuntimeProcType.NON_INCREMENTAL).toDynamic()
        runAnnotationProcessing(srcFiles, listOf(dynamic), generatedSources)

        assertEquals(RuntimeProcType.NON_INCREMENTAL, dynamic.getRuntimeType())
        assertEquals(
            emptyMap<File, File>(),
            dynamic.getGeneratedToSources()
        )
    }
}