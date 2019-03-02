/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test.org.jetbrains.kotlin.kapt3.base.incremental

import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.base.kapt3.collectJavaSourceFiles
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IncrementalKaptTest {

    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testIncrementalRun() {
        val sourcesDir = tmp.newFolder().resolve("test").also { base ->
            base.mkdir()
            listOf("User.java", "Address.java", "Observable.java").map {
                TEST_DATA_DIR.resolve(it).copyTo(base.resolve(it))
            }
        }

        val outputDir = tmp.newFolder()
        val incrementalCacheDir = tmp.newFolder()
        val classpathHistory = tmp.newFolder().also {
            it.resolve("0").createNewFile()
        }
        val options = KaptOptions.Builder().apply {
            projectBaseDir = tmp.newFolder()
            javaSourceRoots.add(sourcesDir)

            sourcesOutputDir = outputDir
            classesOutputDir = outputDir
            stubsOutputDir = outputDir
            incrementalDataOutputDir = outputDir

            incrementalCache = incrementalCacheDir
            classpathFqNamesHistory = classpathHistory
        }.build()

        val logger = WriterBackedKaptLogger(isVerbose = true)
        KaptContext(options, true, logger).use {
            it.doAnnotationProcessing(
                options.collectJavaSourceFiles(it.cacheManager), listOf(SimpleProcessor().toIsolating())
            )
        }

        val classesOutput = tmp.newFolder()
        compileSources(sourcesDir.listFiles().asIterable(), classesOutput)

        val addressTimestamp = outputDir.resolve("test/AddressGenerated.java").lastModified()
        assertTrue(outputDir.resolve("test/UserGenerated.java").exists())
        assertTrue(outputDir.resolve("test/AddressGenerated.java").exists())

        val optionsForSecondRun = KaptOptions.Builder().apply {
            projectBaseDir = tmp.newFolder()

            sourcesOutputDir = outputDir
            classesOutputDir = outputDir
            stubsOutputDir = outputDir
            incrementalDataOutputDir = outputDir

            incrementalCache = incrementalCacheDir
            classpathFqNamesHistory = classpathHistory
            compiledSources.add(classesOutput)
            changedFiles.add(sourcesDir.resolve("User.java"))
        }.build()

        KaptContext(optionsForSecondRun, true, logger).use {
            val sourcesToReprocess = optionsForSecondRun.collectJavaSourceFiles(it.cacheManager)
            assertFalse(outputDir.resolve("test/UserGenerated.java").exists())

            it.doAnnotationProcessing(
                sourcesToReprocess, listOf(SimpleProcessor().toIsolating())
            )
        }

        assertEquals(addressTimestamp, outputDir.resolve("test/AddressGenerated.java").lastModified())
        assertTrue(outputDir.resolve("test/UserGenerated.java").exists())

        sourcesDir.resolve("User.java").delete()
        KaptContext(optionsForSecondRun, true, logger).use {
            it.doAnnotationProcessing(
                optionsForSecondRun.collectJavaSourceFiles(it.cacheManager), listOf(SimpleProcessor().toIsolating())
            )
        }

        assertEquals(addressTimestamp, outputDir.resolve("test/AddressGenerated.java").lastModified())
        assertFalse(outputDir.resolve("test/UserGenerated.java").exists())
    }
}