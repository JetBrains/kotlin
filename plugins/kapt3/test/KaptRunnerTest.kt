import org.jetbrains.kotlin.kapt3.KaptRunner
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.AbstractProcessor
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

class KaptRunnerTest {
    private companion object {
        val TEST_DATA_DIR = File("plugins/kapt3/testData/runner")
    }

    @Test
    fun testSimple() {
        val processor = object : AbstractProcessor() {
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

                return true;
            }

            override fun getSupportedAnnotationTypes() = setOf("test.MyAnnotation")
        }

        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        try {
            KaptRunner().doAnnotationProcessing(
                    listOf(File(TEST_DATA_DIR, "Simple.java")),
                    listOf(processor),
                    sourceOutputDir,
                    sourceOutputDir)
            val myMethodFile = File(sourceOutputDir, "generated/MyMethodMyAnnotation.java")
            assertTrue(myMethodFile.exists())
        } finally {
            sourceOutputDir.deleteRecursively()
        }
    }
}