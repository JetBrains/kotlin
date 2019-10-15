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
import org.jetbrains.kotlin.base.kapt3.DetectMemoryLeaksMode
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt3.AbstractKapt3Extension
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.LoadedProcessors
import org.jetbrains.kotlin.kapt3.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt3.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt3.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt3.prettyPrint
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter.KaptStub
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
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
        val TEST_DATA_DIR = File("plugins/kapt3/kapt3-compiler/testData/kotlinRunner")
    }

    private var _processors: List<Processor>? = null
    private val processors get() = _processors!!

    private var mutableOptions: Map<String, String>? = null

    override fun tearDown() {
        _processors = null
        mutableOptions = null
        super.tearDown()
    }

    protected open fun test(
        name: String,
        vararg supportedAnnotations: String,
        options: Map<String, String> = emptyMap(),
        process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit
    ) = testAP(true, name, options, process, *supportedAnnotations)

    private fun testAP(
        shouldRun: Boolean,
        name: String,
        options: Map<String, String>,
        process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment) -> Unit,
        vararg supportedAnnotations: String
    ) {
        this.mutableOptions = options

        val ktFileName = File(TEST_DATA_DIR, "$name.kt")
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

        _processors = listOf(processor)
        doTest(ktFileName.canonicalPath)

        if (started != shouldRun) {
            fail("Annotation processor " + (if (shouldRun) "was not started" else "was started"))
        }
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".it.txt")

        val javaSources = listOfNotNull(writeJavaFiles(files))
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources.toTypedArray())
        val project = myEnvironment.project

        val options = KaptOptions.Builder().apply {
            projectBaseDir = project.basePath?.let(::File)
            compileClasspath.addAll(PathUtil.getJdkClassesRootsFromCurrentJre() + PathUtil.kotlinPathsForIdeaPlugin.stdlibPath)
            javaSourceRoots.addAll(javaSources)

            sourcesOutputDir = Files.createTempDirectory("kaptRunner").toFile()
            classesOutputDir = sourcesOutputDir
            stubsOutputDir = Files.createTempDirectory("kaptStubs").toFile()
            incrementalDataOutputDir = Files.createTempDirectory("kaptIncrementalData").toFile()

            mutableOptions?.let { processingOptions.putAll(it) }
            detectMemoryLeaks = DetectMemoryLeaksMode.NONE
        }.build()

        val kapt3Extension = Kapt3ExtensionForTests(options, processors)
        AnalysisHandlerExtension.registerExtension(project, kapt3Extension)

        try {
            loadMultiFiles(files)

            val classBuilderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)
            GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory).factory

            val actualRaw = kapt3Extension.savedStubs ?: error("Stubs were not saved")
            val actual = StringUtil.convertLineSeparators(actualRaw.trim { it <= ' ' })
                .trimTrailingWhitespacesAndAddNewlineAtEOF()
                .let { AbstractClassFileToSourceStubConverterTest.removeMetadataAnnotationContents(it) }

            KotlinTestUtils.assertEqualsToFile(txtFile, actual)
        } finally {
            options.sourcesOutputDir.deleteRecursively()
            options.incrementalDataOutputDir?.deleteRecursively()
        }
    }

    protected class Kapt3ExtensionForTests(options: KaptOptions, private val processors: List<Processor>) : AbstractKapt3Extension(
        options, MessageCollectorBackedKaptLogger(options), compilerConfiguration = CompilerConfiguration.EMPTY
    ) {
        internal var savedStubs: String? = null
        internal var savedBindings: Map<String, KaptJavaFileObject>? = null

        override fun loadProcessors() = LoadedProcessors(
            processors.map { IncrementalProcessor(it, DeclaredProcType.NON_INCREMENTAL, logger) },
            Kapt3ExtensionForTests::class.java.classLoader)

        override fun saveStubs(kaptContext: KaptContext, stubs: List<KaptStub>) {
            if (this.savedStubs != null) {
                error("Stubs are already saved")
            }

            this.savedStubs = stubs
                .map { it.file.prettyPrint(kaptContext.context) }
                .sorted()
                .joinToString(AbstractKotlinKapt3Test.FILE_SEPARATOR)

            super.saveStubs(kaptContext, stubs)
        }

        override fun saveIncrementalData(
            kaptContext: KaptContextForStubGeneration,
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
