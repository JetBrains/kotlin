/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.kapt3.base.newCacheFolder
import org.jetbrains.kotlin.kapt3.base.newCompiledSourcesFolder
import org.jetbrains.kotlin.kapt3.base.newGeneratedSourcesFolder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TestSimpleIncrementalAptCache {
    private lateinit var cache: JavaClassCacheManager
    private lateinit var generatedSources: File
    private lateinit var compiledSources: List<File>

    @BeforeEach
    fun setUp(@TempDir tmp: File) {
        cache = JavaClassCacheManager(tmp.newCacheFolder())
        generatedSources = tmp.newGeneratedSourcesFolder()
        compiledSources = listOf(tmp.newCompiledSourcesFolder().also { it.resolve(TEST_PACKAGE_NAME).mkdir() })
        cache.close()
    }

    @Test
    fun testAggregatingAnnotations() {
        runProcessor(SimpleProcessor().toAggregating())

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(
            listOf(TEST_DATA_DIR.resolve("User.java").absoluteFile),
            emptyList(),
            compiledSources
        ) as SourcesToReprocess.Incremental
        assertEquals(
            listOf(TEST_DATA_DIR.resolve("User.java").absoluteFile, TEST_DATA_DIR.resolve("Address.java").absoluteFile),
            dirtyFiles.toReprocess
        )
        assertFalse(generatedSources.resolve("test/UserGenerated.java").exists())
        assertFalse(generatedSources.resolve("test/AddressGenerated.java").exists())
    }

    @Test
    fun testIsolatingAnnotations() {
        runProcessor(SimpleProcessor().toIsolating())

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(
            listOf(TEST_DATA_DIR.resolve("User.java").absoluteFile),
            emptyList(),
            compiledSources
        ) as SourcesToReprocess.Incremental
        assertFalse(generatedSources.resolve("test/UserGenerated.java").exists())
        assertEquals(
            listOf(TEST_DATA_DIR.resolve("User.java").absoluteFile),
            dirtyFiles.toReprocess
        )
    }

    @Test
    fun testNonIncremental() {
        runProcessor(SimpleProcessor().toNonIncremental())

        val dirtyFiles = cache.invalidateAndGetDirtyFiles(listOf(TEST_DATA_DIR.resolve("User.java").absoluteFile), emptyList(), compiledSources)
        assertTrue(dirtyFiles is SourcesToReprocess.FullRebuild)
    }

    private fun runProcessor(processor: IncrementalProcessor) {
        val srcFiles = listOf("User.java", "Address.java", "Observable.java").map { File(TEST_DATA_DIR, it) }
        runAnnotationProcessing(
            srcFiles,
            listOf(processor),
            generatedSources
        ) { elementUtils, trees -> MentionedTypesTaskListener(cache.javaCache, elementUtils, trees) }
        cache.updateCache(listOf(processor), false)

        // add mock compiled source files
        compiledSources.single().resolve("test/User.class").createNewFile()
        compiledSources.single().resolve("test/Address.class").createNewFile()
        compiledSources.single().resolve("test/Observable.class").createNewFile()
        compiledSources.single().resolve("test/UserGenerated.class").createNewFile()
        compiledSources.single().resolve("test/AddressGenerated.class").createNewFile()
    }
}

private const val TEST_PACKAGE_NAME = "test"
