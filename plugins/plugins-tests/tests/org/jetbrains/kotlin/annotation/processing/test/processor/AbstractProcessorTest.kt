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

package org.jetbrains.kotlin.annotation.processing.test.processor

import com.intellij.openapi.extensions.Extensions
import com.intellij.testFramework.registerServiceInstance
import org.jetbrains.kotlin.annotation.AbstractAnnotationProcessingExtension
import org.jetbrains.kotlin.annotation.processing.diagnostic.DefaultErrorMessagesAnnotationProcessing
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.AbstractBytecodeTextTest
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.java.model.elements.JeAnnotationMirror
import org.jetbrains.kotlin.java.model.elements.JeMethodExecutableElement
import org.jetbrains.kotlin.java.model.elements.JeTypeElement
import org.jetbrains.kotlin.java.model.elements.JeVariableElement
import org.jetbrains.kotlin.java.model.internal.JeElementRegistry
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.Completion
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*

class AnnotationProcessingExtensionForTests(
        val processors: List<Processor>
) : AbstractAnnotationProcessingExtension(createTempDir(), createTempDir(), listOf(), true,
                                          createIncrementalDataFile()) {
    override fun loadAnnotationProcessors() = processors

    override val options: Map<String, String>
        get() = emptyMap()

    private companion object {
        fun createTempDir(): File = Files.createTempDirectory("ap-test").toFile().apply {
            deleteOnExit()
        }
        
        fun createIncrementalDataFile(): File = File.createTempFile("incrementalData", "txt").apply {
            deleteOnExit()
        }
    }
}

abstract class AbstractProcessorTest : AbstractBytecodeTextTest() {
    abstract val testDataDir: String
    
    private fun createTestEnvironment(processors: List<Processor>): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project
        
        val apExtension = AnnotationProcessingExtensionForTests(processors)
        AnalysisHandlerExtension.registerExtension(project, apExtension)

        project.registerServiceInstance(JeElementRegistry::class.java, JeElementRegistry())

        Extensions.getRootArea().getExtensionPoint(DefaultErrorMessages.Extension.EP_NAME)
                .registerExtension(DefaultErrorMessagesAnnotationProcessing())

        return environment
    }

    fun doTest(path: String, processors: List<Processor>) {
        myEnvironment = createTestEnvironment(processors)
        
        loadFileByFullPath(path)
        CodegenTestUtil.generateFiles(myEnvironment, myFiles)
    }

    protected fun Element.assertHasAnnotation(fqName: String, vararg parameterValues: Any?) {
        val annotation = annotationMirrors.first { it is JeAnnotationMirror && it.psi.qualifiedName == fqName }
        val actualValues = annotation.elementValues.values
        assertEquals(parameterValues.size, actualValues.size)

        for ((expected, actual) in parameterValues.zip(actualValues)) {
            assertEquals(expected, actual.value)
        }
    }

    protected fun test(
            name: String,
            vararg supportedAnnotations: String,
            process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit
    ) = testAP(true, name, process, *supportedAnnotations)

    protected fun testShouldNotRun(
            name: String,
            vararg supportedAnnotations: String
    ) = testAP(false, name, { set, roundEnv, env -> fail("Should not run") }, *supportedAnnotations)

    protected fun testAP(
            shouldRun: Boolean,
            name: String,
            process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit,
            vararg supportedAnnotations: String
    ) {
        val ktFileName = File(testDataDir, name + ".kt")
        var started = false
        val processor = object : Processor {
            lateinit var processingEnv: ProcessingEnvironment
            
            override fun getSupportedOptions() = setOf<String>()

            override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
                if (!roundEnv.processingOver()) {
                    started = true
                    process(annotations, roundEnv, processingEnv)
                }
                return true
            }

            override fun init(env: ProcessingEnvironment) {
                processingEnv = env
            }

            override fun getCompletions(
                    element: Element?,
                    annotation: AnnotationMirror?,
                    member: ExecutableElement?,
                    userText: String?
            ): Iterable<Completion>? {
                return emptyList()
            }

            override fun getSupportedSourceVersion() = SourceVersion.RELEASE_6
            override fun getSupportedAnnotationTypes() = supportedAnnotations.toSet()
        }

        doTest(ktFileName.canonicalPath, listOf(processor))
        
        if (started != shouldRun) {
            fail("Annotation processor " + (if (shouldRun) "was not started" else "was started"))
        }
    }

    protected fun TypeElement.findMethods(name: String): List<JeMethodExecutableElement> {
        return enclosedElements.filterIsInstance<JeMethodExecutableElement>().filter { it.simpleName.toString() == name }
    }

    protected fun TypeElement.findMethod(name: String, vararg parameterTypes: String): JeMethodExecutableElement {
        return enclosedElements.first {
            if (it !is JeMethodExecutableElement
                || it.simpleName.toString() != name 
                || parameterTypes.size != it.parameters.size) return@first false
            parameterTypes.zip(it.parameters).all { it.first == it.second.asType().toString() }
        } as JeMethodExecutableElement
    }

    protected fun TypeElement.findField(name: String): JeVariableElement {
        return enclosedElements.first { it is JeVariableElement && it.simpleName.toString() == name } as JeVariableElement
    }

    protected fun ProcessingEnvironment.findClass(fqName: String) = elementUtils.getTypeElement(fqName) as JeTypeElement

    protected fun assertEquals(expected: String, actual: Name) = assertEquals(expected, actual.toString())
}