/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.annotation

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.io.StringWriter
import java.io.Writer
import org.junit.Assert.*

public abstract class AbstractAnnotationProcessorBoxTest : CodegenTestCase() {

    public fun doTest(path: String) {
        val testName = getTestName(true)
        val fileName = path + testName + ".kt"
        val supportInheritedAnnotations = testName.startsWith("inherited")

        val collectorExtension = createTestEnvironment(supportInheritedAnnotations)
        loadFileByFullPath(fileName)
        CodegenTestUtil.generateFiles(myEnvironment, myFiles)

        val actualAnnotations = JetTestUtils.replaceHashWithStar(collectorExtension.stringWriter.toString())
        val expectedAnnotationsFile = File(path + "annotations.txt")

        JetTestUtils.assertEqualsToFile(expectedAnnotationsFile, actualAnnotations)
    }

    override fun codegenTestBasePath(): String {
        return "plugins/annotation-collector/testData/codegen/"
    }

    fun createTestEnvironment(supportInheritedAnnotations: Boolean): AnnotationCollectorExtensionForTests {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
        val environment = KotlinCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project

        val collectorExtension = AnnotationCollectorExtensionForTests(supportInheritedAnnotations)
        ClassBuilderInterceptorExtension.registerExtension(project, collectorExtension)

        myEnvironment = environment

        return collectorExtension
    }

    private class AnnotationCollectorExtensionForTests(
            supportInheritedAnnotations: Boolean
    ) : AnnotationCollectorExtensionBase(supportInheritedAnnotations) {
        val stringWriter = StringWriter()

        override fun getWriter(diagnostic: DiagnosticSink) = stringWriter
        override fun closeWriter() {}

        override val annotationFilterList = listOf<String>()
    }

}