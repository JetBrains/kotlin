/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt3.test

import com.intellij.openapi.command.impl.DummyProject
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.kapt3.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement

/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class JavaKaptContextTest {
    companion object {
        private val TEST_DATA_DIR = File("plugins/kapt3/testData/runner")
        val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)

        fun simpleProcessor() = object : AbstractProcessor() {
            override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                for (annotation in annotations) {
                    val annotationName = annotation.simpleName.toString()
                    val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation)

                    for (annotatedElement in annotatedElements) {
                        val generatedClassName = annotatedElement.simpleName.toString().capitalize() + annotationName.capitalize()
                        val file = processingEnv.filer.createSourceFile("generated." + generatedClassName)
                        file.openWriter().use {
                            it.write("""
                            package generated;
                            class $generatedClassName {}
                            """.trimIndent())
                        }
                    }
                }

                return true
            }

            override fun getSupportedAnnotationTypes() = setOf("test.MyAnnotation")
        }
    }

    private fun doAnnotationProcessing(javaSourceFile: File, processor: Processor, outputDir: File) {
        KaptContext(KaptLogger(isVerbose = true, messageCollector = messageCollector),
                    DummyProject.getInstance(),
                    bindingContext = BindingContext.EMPTY,
                    compiledClasses = emptyList(),
                    origins = emptyMap(),
                    generationState = null,
                    processorOptions = emptyMap()
        ).doAnnotationProcessing(
                listOf(javaSourceFile),
                listOf(processor),
                emptyList(), // compile classpath
                emptyList(), // annotation processing classpath
                "", // list of annotation processor qualified names
                outputDir,
                outputDir,
                withJdk = true)
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

    @Test(expected = KaptError::class)
    fun testException() {
        val exceptionMessage = "Here we are!"

        val processor = object : AbstractProcessor() {
            override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                throw RuntimeException(exceptionMessage)
            }

            override fun getSupportedAnnotationTypes() = setOf("test.MyAnnotation")
        }

        try {
            doAnnotationProcessing(File(TEST_DATA_DIR, "Simple.java"), processor, TEST_DATA_DIR)
        } catch (e: KaptError) {
            assertEquals(KaptError.Kind.EXCEPTION, e.kind)
            assertEquals("Here we are!", e.cause!!.message)
            throw e
        }
    }

    @Test(expected = KaptError::class)
    fun testParsingError() {
        try {
            doAnnotationProcessing(File(TEST_DATA_DIR, "ParseError.java"), simpleProcessor(), TEST_DATA_DIR)
        } catch (e: KaptError) {
            assertEquals(KaptError.Kind.ERROR_RAISED, e.kind)
            throw e
        }
    }
}