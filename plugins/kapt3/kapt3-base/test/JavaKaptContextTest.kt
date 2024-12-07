/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.base.test

import org.jetbrains.kotlin.kapt3.base.*
import org.jetbrains.kotlin.kapt3.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt3.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt3.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt3.base.util.WriterBackedKaptLogger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

class JavaKaptContextTest {
    companion object {
        private val TEST_DATA_DIR = File("plugins/kapt3/kapt3-base/testData/runner")
        val logger = WriterBackedKaptLogger(isVerbose = true)

        fun simpleProcessor() = IncrementalProcessor(
            object : AbstractProcessor() {
                override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                    for (annotation in annotations) {
                        val annotationName = annotation.simpleName.toString()
                        val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)

                        for (annotatedElement in annotatedElements) {
                            val generatedClassName = annotatedElement.simpleName.toString().replaceFirstChar(Char::uppercaseChar) +
                                    annotationName.replaceFirstChar(Char::uppercaseChar)
                            val file = processingEnv.filer.createSourceFile("generated.$generatedClassName")
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
            }, DeclaredProcType.NON_INCREMENTAL, logger
        )
    }

    private fun doAnnotationProcessing(
        javaSourceFile: File,
        processor: IncrementalProcessor,
        outputDir: File,
        fileReadOutput: File? = null
    ) {
        val options = KaptOptions.Builder().apply {
            projectBaseDir = javaSourceFile.parentFile

            sourcesOutputDir = outputDir
            classesOutputDir = outputDir
            stubsOutputDir = outputDir
            incrementalDataOutputDir = outputDir

            fileReadHistoryReportFile = fileReadOutput

            flags.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)
            detectMemoryLeaks = DetectMemoryLeaksMode.NONE
        }.build()

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
    fun testDumpFileReadHistory() {
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        val fileReadOutputFile = File.createTempFile("kapt_read_history", ".txt")
        try {
            doAnnotationProcessing(
                File(TEST_DATA_DIR, "Simple.java"),
                simpleProcessor(),
                sourceOutputDir,
                fileReadOutput = fileReadOutputFile
            )
            assertTrue(fileReadOutputFile.exists())
            assertTrue(fileReadOutputFile.readText().contains("generated/MyMethodMyAnnotation.java"))
            assertTrue(fileReadOutputFile.readText().contains("java/lang/Enum.class"))
        } finally {
            sourceOutputDir.deleteRecursively()
            fileReadOutputFile.delete()
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
            doAnnotationProcessing(
                File(TEST_DATA_DIR, "Simple.java"),
                IncrementalProcessor(processor, DeclaredProcType.NON_INCREMENTAL, logger),
                TEST_DATA_DIR
            )
        } catch (e: KaptBaseError) {
            assertEquals(KaptBaseError.Kind.EXCEPTION, e.kind)
            assertEquals("Here we are!", e.cause!!.message)
            triggered = true
        } catch (e: Throwable) { // AnnotationProcessorError
            assertTrue(e.cause!!.message!!.contains("Here we are!"))
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
