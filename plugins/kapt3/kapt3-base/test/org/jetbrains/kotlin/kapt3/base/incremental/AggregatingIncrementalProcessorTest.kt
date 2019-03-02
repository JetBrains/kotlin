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

class AggregatingIncrementalProcessorTest {
    @JvmField
    @Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    private lateinit var generatedSources: File

    @Before
    fun setUp() {
        generatedSources = tmp.newFolder()
    }

    @Test
    fun testDependenciesRecorded() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val aggregating = SimpleProcessor().toAggregating()
        runAnnotationProcessing(srcFiles, listOf(aggregating), generatedSources)

        assertEquals(RuntimeProcType.AGGREGATING, aggregating.getRuntimeType())

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to null,
                generatedSources.resolve("test/AddressGenerated.java") to null
            ),
            aggregating.getGeneratedToSources()
        )
    }

    @Test
    fun testNoSourcesToProcess() {
        val srcFiles = listOf("Observable.java").map { File(TEST_DATA_DIR, it) }
        val aggregating = SimpleProcessor().toAggregating()
        runAnnotationProcessing(srcFiles, listOf(aggregating), generatedSources)

        assertEquals(RuntimeProcType.AGGREGATING, aggregating.getRuntimeType())
        assertEquals(emptyMap<File, File>(), aggregating.getGeneratedToSources())
    }

    @Test
    fun testGeneratingSourcesClassesResources() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val aggregating = SimpleCreatingClassFilesAndResources().toAggregating()
        runAnnotationProcessing(srcFiles, listOf(aggregating), generatedSources)

        assertEquals(RuntimeProcType.AGGREGATING, aggregating.getRuntimeType())

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to null,
                generatedSources.resolve("test/UserGeneratedClass.class") to null,
                generatedSources.resolve("test/UserGeneratedResource") to null,
                generatedSources.resolve("test/AddressGenerated.java") to null,
                generatedSources.resolve("test/AddressGeneratedClass.class") to null,
                generatedSources.resolve("test/AddressGeneratedResource") to null
            ),
            aggregating.getGeneratedToSources()
        )
    }
}