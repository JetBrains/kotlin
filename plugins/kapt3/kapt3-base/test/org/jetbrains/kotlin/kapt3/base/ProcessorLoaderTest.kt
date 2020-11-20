/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProcessorLoaderTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    @Test
    fun testProcessorClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            val jar = tmp.newFile("empty.jar").also {
                ZipOutputStream(it.outputStream()).use {
                    it.putNextEntry(ZipEntry("fake_entry"))
                    it.closeEntry()
                }
            }
            processingClasspath.add(jar)
            sourcesOutputDir = tmp.newFolder()
            classesOutputDir = tmp.newFolder()
            stubsOutputDir = tmp.newFolder()
            build()
        }
        val loadedProcessors = ProcessorLoader(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        Assert.assertTrue(loadedProcessors.processors.isEmpty())
    }

    @Test
    fun testProcessorUpperCaseExtensionClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            val jar = tmp.newFile("empty.JAR").also {
                ZipOutputStream(it.outputStream()).use {
                    it.putNextEntry(ZipEntry("fake_entry"))
                    it.closeEntry()
                }
            }
            processingClasspath.add(jar)
            sourcesOutputDir = tmp.newFolder()
            classesOutputDir = tmp.newFolder()
            stubsOutputDir = tmp.newFolder()
            build()
        }
        val loadedProcessors = ProcessorLoader(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        Assert.assertTrue(loadedProcessors.processors.isEmpty())
    }

    @Test
    fun testEmptyClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            sourcesOutputDir = tmp.newFolder()
            classesOutputDir = tmp.newFolder()
            stubsOutputDir = tmp.newFolder()
            build()
        }
        val loadedProcessors = ProcessorLoader(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        Assert.assertTrue(loadedProcessors.processors.isEmpty())
    }

    @Test
    fun testClasspathWithNonJars() {
        val kaptOptions = with(KaptOptions.Builder()) {
            processingClasspath.add(tmp.newFile("do-not-load.gz"))
            sourcesOutputDir = tmp.newFolder()
            classesOutputDir = tmp.newFolder()
            stubsOutputDir = tmp.newFolder()
            build()
        }
        val loadedProcessors = ProcessorLoader(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        Assert.assertTrue(loadedProcessors.processors.isEmpty())
    }
}