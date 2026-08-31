/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test

import org.jetbrains.kotlin.kapt.base.KaptFlag
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.ProcessorLoaderImpl
import org.jetbrains.kotlin.kapt.base.util.WriterBackedKaptLogger
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProcessorLoaderTest {
    @Suppress("PropertyName")
    @JvmField
    @TempDir
    var _rootTempDirectory: File? = null

    val rootTempDirectory: File
        get() = _rootTempDirectory!!

    @Test
    fun testProcessorClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            val jar = rootTempDirectory.newFile("empty.jar").also { file ->
                ZipOutputStream(file.outputStream()).use {
                    it.putNextEntry(ZipEntry("fake_entry"))
                    it.closeEntry()
                }
            }
            processingClasspath.add(jar)
            sourcesOutputDir = rootTempDirectory.newSourcesFolder()
            classesOutputDir = rootTempDirectory.newClassesFolder()
            stubsOutputDir = rootTempDirectory.newStubsFolder()
            build()
        }
        val loadedProcessors = ProcessorLoaderImpl(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        assertTrue(loadedProcessors.processors.isEmpty())
    }

    @Test
    fun testProcessorUpperCaseExtensionClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            val jar = rootTempDirectory.newFile("empty.JAR").also { file ->
                ZipOutputStream(file.outputStream()).use {
                    it.putNextEntry(ZipEntry("fake_entry"))
                    it.closeEntry()
                }
            }
            processingClasspath.add(jar)
            sourcesOutputDir = rootTempDirectory.newSourcesFolder()
            classesOutputDir = rootTempDirectory.newClassesFolder()
            stubsOutputDir = rootTempDirectory.newStubsFolder()
            build()
        }
        val loadedProcessors = ProcessorLoaderImpl(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        assertTrue(loadedProcessors.processors.isEmpty())
    }

    @Test
    fun testEmptyClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            sourcesOutputDir = rootTempDirectory.newSourcesFolder()
            classesOutputDir = rootTempDirectory.newClassesFolder()
            stubsOutputDir = rootTempDirectory.newStubsFolder()
            build()
        }
        val loadedProcessors = ProcessorLoaderImpl(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        assertTrue(loadedProcessors.processors.isEmpty())
    }

    @Test
    fun testIsolatedClassLoaderHidesBuildClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            flags.add(KaptFlag.ISOLATE_PROCESSORS_FROM_BUILD_CLASSPATH)
            sourcesOutputDir = rootTempDirectory.newSourcesFolder()
            classesOutputDir = rootTempDirectory.newClassesFolder()
            stubsOutputDir = rootTempDirectory.newStubsFolder()
            build()
        }
        ProcessorLoaderImpl(kaptOptions, WriterBackedKaptLogger(false)).use { processorLoader ->
            val classLoader = processorLoader.loadProcessors().classLoader

            // Classes of the hosting process must not be visible to annotation processors (KT-88583).
            assertThrows(ClassNotFoundException::class.java) { Class.forName("kotlin.Unit", false, classLoader) }
            assertThrows(ClassNotFoundException::class.java) { Class.forName("org.junit.jupiter.api.Test", false, classLoader) }

            // JDK platform classes and javac must stay visible and be shared with the hosting process.
            assertSame(javax.annotation.processing.Processor::class.java, Class.forName("javax.annotation.processing.Processor", false, classLoader))
            assertSame(javax.lang.model.element.TypeElement::class.java, Class.forName("javax.lang.model.element.TypeElement", false, classLoader))
            assertSame(com.sun.tools.javac.util.Context::class.java, Class.forName("com.sun.tools.javac.util.Context", false, classLoader))
            assertSame(com.sun.source.util.Trees::class.java, Class.forName("com.sun.source.util.Trees", false, classLoader))
        }
    }

    @Test
    fun testNonIsolatedClassLoaderSeesBuildClasspath() {
        val kaptOptions = with(KaptOptions.Builder()) {
            sourcesOutputDir = rootTempDirectory.newSourcesFolder()
            classesOutputDir = rootTempDirectory.newClassesFolder()
            stubsOutputDir = rootTempDirectory.newStubsFolder()
            build()
        }
        ProcessorLoaderImpl(kaptOptions, WriterBackedKaptLogger(false)).use { processorLoader ->
            val classLoader = processorLoader.loadProcessors().classLoader
            assertSame(Unit::class.java, Class.forName("kotlin.Unit", false, classLoader))
        }
    }

    @Test
    fun testClasspathWithNonJars() {
        val kaptOptions = with(KaptOptions.Builder()) {
            processingClasspath.add(rootTempDirectory.newFile("do-not-load.gz"))
            sourcesOutputDir = rootTempDirectory.newSourcesFolder()
            classesOutputDir = rootTempDirectory.newClassesFolder()
            stubsOutputDir = rootTempDirectory.newStubsFolder()
            build()
        }
        val loadedProcessors = ProcessorLoaderImpl(kaptOptions, WriterBackedKaptLogger(false)).loadProcessors()
        assertTrue(loadedProcessors.processors.isEmpty())
    }
}
