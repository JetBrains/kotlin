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
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.AbstractKapt3Extension
import org.jetbrains.kotlin.kapt3.AptMode.STUBS_AND_APT
import org.jetbrains.kotlin.kapt3.Kapt3BuilderFactory
import org.jetbrains.kotlin.kapt3.KaptContext
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.util.KaptLogger
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.Files
import javax.annotation.processing.Completion
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import com.sun.tools.javac.util.List as JavacList

abstract class AbstractKotlinKapt3IntegrationTest : CodegenTestCase() {
    private companion object {
        val TEST_DATA_DIR = File("plugins/kapt3/testData/kotlinRunner")
    }

    private lateinit var processors: List<Processor>
    private lateinit var options: Map<String, String>

    protected fun test(
            name: String,
            vararg supportedAnnotations: String,
            options: Map<String, String> = emptyMap(),
            process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit
    ) = testAP(true, name, options, process, *supportedAnnotations)

    protected fun testAP(
            shouldRun: Boolean,
            name: String,
            options: Map<String, String>,
            process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit,
            vararg supportedAnnotations: String
    ) {
        this.options = options

        val ktFileName = File(TEST_DATA_DIR, name + ".kt")
        var started = false
        val processor = object : Processor {
            lateinit var processingEnv: ProcessingEnvironment

            override fun getSupportedOptions() = options.keys

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

        processors = listOf(processor)
        doTest(ktFileName.canonicalPath)

        if (started != shouldRun) {
            fail("Annotation processor " + (if (shouldRun) "was not started" else "was started"))
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".it.txt")
        val sourceOutputDir = Files.createTempDirectory("kaptRunner").toFile()
        val stubsDir = Files.createTempDirectory("kaptStubs").toFile()
        val incrementalDataDir = Files.createTempDirectory("kaptIncrementalData").toFile()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)

        val kapt3Extension = Kapt3ExtensionForTests(processors, javaSources.toList(), sourceOutputDir, this.options,
                                                    stubsOutputDir = stubsDir, incrementalDataOutputDir = incrementalDataDir)

        AnalysisHandlerExtension.registerExtension(myEnvironment.project, kapt3Extension)

        try {
            loadMultiFiles(files)

            GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, Kapt3BuilderFactory()).factory

            val actualRaw = kapt3Extension.savedStubs ?: error("Stubs were not saved")
            val actual = StringUtil.convertLineSeparators(actualRaw.trim({ it <= ' ' }))
                    .trimTrailingWhitespacesAndAddNewlineAtEOF()
                    .let { AbstractClassFileToSourceStubConverterTest.removeMetadataAnnotationContents(it) }

            KotlinTestUtils.assertEqualsToFile(txtFile, actual)
        } finally {
            sourceOutputDir.deleteRecursively()
            incrementalDataDir.deleteRecursively()
        }
    }

    protected class Kapt3ExtensionForTests(
            private val processors: List<Processor>,
            javaSourceRoots: List<File>,
            outputDir: File,
            options: Map<String, String>,
            stubsOutputDir: File,
            incrementalDataOutputDir: File
    ) : AbstractKapt3Extension(PathUtil.getJdkClassesRootsFromCurrentJre() + PathUtil.kotlinPathsForIdeaPlugin.stdlibPath,
                               emptyList(), javaSourceRoots, outputDir, outputDir,
                               stubsOutputDir, incrementalDataOutputDir, options, emptyMap(), "", STUBS_AND_APT, System.currentTimeMillis(),
                               KaptLogger(true), correctErrorTypes = true, compilerConfiguration = CompilerConfiguration.EMPTY
    ) {
        internal var savedStubs: String? = null
        internal var savedBindings: Map<String, KaptJavaFileObject>? = null

        override fun loadProcessors() = processors

        override fun saveStubs(stubs: JavacList<JCTree.JCCompilationUnit>) {
            if (this.savedStubs != null) {
                error("Stubs are already saved")
            }

            this.savedStubs = stubs
                    .map { it.toString() }
                    .sorted()
                    .joinToString(AbstractKotlinKapt3Test.FILE_SEPARATOR)

            super.saveStubs(stubs)
        }

        override fun saveIncrementalData(
                kaptContext: KaptContext<GenerationState>,
                messageCollector: MessageCollector,
                converter: ClassFileToSourceStubConverter
        ) {
            if (this.savedBindings != null) {
                error("Bindings are already saved")
            }

            this.savedBindings = converter.bindings

            super.saveIncrementalData(kaptContext, messageCollector, converter)
        }
    }
}
