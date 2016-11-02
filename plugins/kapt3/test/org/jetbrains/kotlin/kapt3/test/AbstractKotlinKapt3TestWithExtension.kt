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

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.kapt3.AbstractKapt3Extension
import org.jetbrains.kotlin.kapt3.Kapt3BuilderFactory
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.Processor

class KotlinKapt3TestWithExtension : CodegenTestCase() {
    class Kapt3ExtensionForTests(
            private val processors: List<Processor>,
            javaSourceRoots: List<File>,
            outputDir: File
    ) : AbstractKapt3Extension(emptyList(), javaSourceRoots, outputDir, outputDir,
                               null, emptyMap(), true, System.currentTimeMillis(), KaptLogger(true)) {
        override fun loadProcessors() = processors
    }

    private fun getProcessor(): Processor = KaptRunnerTest.simpleProcessor()

    @Test
    fun testSimple() {
        doTest("plugins/kapt3/testData/kotlinRunner/Simple.kt")
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".ext.txt")
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)
        val kapt3Extension = Kapt3ExtensionForTests(listOf(getProcessor()), javaSources.toList(), sourceOutputDir)
        AnalysisHandlerExtension.registerExtension(myEnvironment.project, kapt3Extension)

        try {
            loadMultiFiles(files)

            val classBuilderFactory = Kapt3BuilderFactory()
            CodegenTestUtil.generateFiles(myEnvironment, myFiles, classBuilderFactory)

            val javaFiles = sourceOutputDir.walkTopDown().filter { it.isFile && it.extension == "java" }
            val actualRaw = javaFiles.sortedBy { it.name }.joinToString(AbstractKotlinKapt3Test.FILE_SEPARATOR) { it.name + ":\n\n" + it.readText() }
            val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()
            KotlinTestUtils.assertEqualsToFile(txtFile, actual)
        } finally {
            sourceOutputDir.deleteRecursively()
        }
    }

}