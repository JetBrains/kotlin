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

class IsolationgIncrementalProcessorTest {
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
        val isolating = SimpleProcessor().toIsolating()
        runAnnotationProcessing(srcFiles, listOf(isolating), generatedSources)

        assertEquals(RuntimeProcType.ISOLATING, isolating.getRuntimeType())

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/User.java").absoluteFile,
                generatedSources.resolve("test/AddressGenerated.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/Address.java").absoluteFile
            ),
            isolating.getGeneratedToSources()
        )
    }

    @Test
    fun testNoSourcesToProcess() {
        val srcFiles = listOf("Observable.java").map { File(TEST_DATA_DIR, it) }
        val isolating = SimpleProcessor().toIsolating()
        runAnnotationProcessing(srcFiles, listOf(isolating), generatedSources)

        assertEquals(RuntimeProcType.ISOLATING, isolating.getRuntimeType())
        assertEquals(emptyMap<File, File>(), isolating.getGeneratedToSources())
    }

    @Test
    fun testGeneratingSourcesClassesResources() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val isolating = SimpleCreatingClassFilesAndResources().toIsolating()
        runAnnotationProcessing(srcFiles, listOf(isolating), generatedSources)

        assertEquals(RuntimeProcType.ISOLATING, isolating.getRuntimeType())

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to TEST_DATA_DIR.resolve("User.java").absoluteFile,
                generatedSources.resolve("test/UserGeneratedClass.class") to TEST_DATA_DIR.resolve("User.java").absoluteFile,
                generatedSources.resolve("test/UserGeneratedResource") to TEST_DATA_DIR.resolve("User.java").absoluteFile,
                generatedSources.resolve("test/AddressGenerated.java") to TEST_DATA_DIR.resolve("Address.java").absoluteFile,
                generatedSources.resolve("test/AddressGeneratedClass.class") to TEST_DATA_DIR.resolve("Address.java").absoluteFile,
                generatedSources.resolve("test/AddressGeneratedResource") to TEST_DATA_DIR.resolve("Address.java").absoluteFile
            ),
            isolating.getGeneratedToSources()
        )
    }

    @Test
    fun testWrongOriginElement() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val isolating = SimpleProcessor(wrongOrigin = true).toIsolating()
        runAnnotationProcessing(srcFiles, listOf(isolating), generatedSources)

        assertEquals(RuntimeProcType.NON_INCREMENTAL, isolating.getRuntimeType())
        assertEquals(emptyMap<File, File>(), isolating.getGeneratedToSources())
    }

    @Test
    fun testTwoIsolating() {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        val isolating = listOf(
            SimpleProcessor().toIsolating(),
            SimpleProcessor(generatedSuffix = "Two").toIsolating()
        )
        runAnnotationProcessing(srcFiles, isolating, generatedSources)

        isolating.forEach { assertEquals(RuntimeProcType.ISOLATING, it.getRuntimeType()) }
        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGenerated.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/User.java").absoluteFile,
                generatedSources.resolve("test/AddressGenerated.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/Address.java").absoluteFile
            ), isolating[0].getGeneratedToSources()
        )

        assertEquals(
            mapOf(
                generatedSources.resolve("test/UserGeneratedTwo.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/User.java").absoluteFile,
                generatedSources.resolve("test/AddressGeneratedTwo.java") to File("plugins/kapt3/kapt3-base/testData/runner/incremental/Address.java").absoluteFile
            ), isolating[1].getGeneratedToSources()
        )
    }
}
