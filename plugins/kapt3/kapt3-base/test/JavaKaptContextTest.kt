/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test

import junit.framework.TestCase
import org.jetbrains.kotlin.base.kapt3.DetectMemoryLeaksMode
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt3.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt3.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class JavaKaptContextTest : TestCase() {
    companion object {
        private val TEST_DATA_DIR = File("plugins/kapt3/kapt3-base/testData/runner")

        fun simpleProcessor() = IncrementalProcessor(
            object : AbstractProcessor() {
                override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                    for (annotation in annotations) {
                        val annotationName = annotation.simpleName.toString()
                        val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)

                        for (annotatedElement in annotatedElements) {
                            val generatedClassName = annotatedElement.simpleName.toString().capitalize() + annotationName.capitalize()
                            val file = processingEnv.filer.createSourceFile("generated." + generatedClassName)
                            file.openWriter().use {
                                it.write(
                                    """
                            package generated;
                            class $generatedClassName {}
                            """.trimIndent()
                                )
                            }
                        }
                    }

                    return true
                }

                override fun getSupportedAnnotationTypes() = setOf("test.MyAnnotation")
            }, DeclaredProcType.NON_INCREMENTAL
        )
    }

    private fun doAnnotationProcessing(javaSourceFile: File, processor: IncrementalProcessor, outputDir: File) {
        val options = KaptOptions.Builder().apply {
            projectBaseDir = javaSourceFile.parentFile

            sourcesOutputDir = outputDir
            classesOutputDir = outputDir
            stubsOutputDir = outputDir
            incrementalDataOutputDir = outputDir

            flags.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)
            detectMemoryLeaks = DetectMemoryLeaksMode.NONE
        }.build()

        val logger = WriterBackedKaptLogger(isVerbose = true)
        KaptContext(options, true, logger).doAnnotationProcessing(listOf(javaSourceFile), listOf(processor))
    }

    @Test
    fun testSimple() {
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        try {
            doAnnotationProcessing(File(TEST_DATA_DIR, "Simple.java"), simpleProcessor(), sourceOutputDir)
            val myMethodFile = File(sourceOutputDir, "generated/MyMethodMyAnnotation.java")
            assertTrue(myMethodFile.exists())
        } finally {
            sourceOutputDir.deleteRecursively()
        }
    }

    @Test
    fun testException() {
        val exceptionMessage = "Here we are!"
        var triggered = false

        val processor = object : AbstractProcessor() {
            override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                throw RuntimeException(exceptionMessage)
            }

            override fun getSupportedAnnotationTypes() = setOf("test.MyAnnotation")
        }

        try {
            doAnnotationProcessing(File(TEST_DATA_DIR, "Simple.java"), IncrementalProcessor(processor, DeclaredProcType.NON_INCREMENTAL), TEST_DATA_DIR)
        } catch (e: KaptBaseError) {
            assertEquals(KaptBaseError.Kind.EXCEPTION, e.kind)
            assertEquals("Here we are!", e.cause!!.message)
            triggered = true
        }

        assertTrue(triggered)
    }

    @Test
    fun testParsingError() {
        var triggered = false

        try {
            doAnnotationProcessing(File(TEST_DATA_DIR, "ParseError.java"), simpleProcessor(), TEST_DATA_DIR)
        } catch (e: KaptBaseError) {
            assertEquals(KaptBaseError.Kind.ERROR_RAISED, e.kind)
            triggered = true
        }

        assertTrue(triggered)
    }
}