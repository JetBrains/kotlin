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
import com.sun.tools.javac.comp.CompileStates
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.kapt3.JCTreeConverter
import org.jetbrains.kotlin.kapt3.Kapt3BuilderFactory
import org.jetbrains.kotlin.kapt3.KaptRunner
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File

abstract class AbstractJCTreeConverterTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)
        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val classBuilderFactory = Kapt3BuilderFactory()
        val factory = CodegenTestUtil.generateFiles(myEnvironment, myFiles, classBuilderFactory)
        val typeMapper = factory.generationState.typeMapper

        val kaptRunner = KaptRunner()
        try {
            val converter = JCTreeConverter(kaptRunner.context, typeMapper, classBuilderFactory.compiledClasses, classBuilderFactory.origins)
            val javaFiles = converter.convert()

            kaptRunner.compiler.enterTrees(javaFiles)

            val actualRaw = javaFiles.joinToString ("\n\n////////////////////\n\n")
            val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' })).trimTrailingWhitespacesAndAddNewlineAtEOF()

            if (kaptRunner.compiler.shouldStop(CompileStates.CompileState.ENTER)) {
                error("There were errors during analysis. See errors above. Stubs:\n\n$actual")
            }

            KotlinTestUtils.assertEqualsToFile(txtFile, actual)
        } finally {
            kaptRunner.compiler.close()
        }
    }
}

