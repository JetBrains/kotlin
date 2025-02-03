/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.test.integration

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.kapt.AbstractKaptExtension
import org.jetbrains.kotlin.kapt.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt.base.KaptOptions
import org.jetbrains.kotlin.kapt.base.LoadedProcessors
import org.jetbrains.kotlin.kapt.base.incremental.DeclaredProcType
import org.jetbrains.kotlin.kapt.base.incremental.IncrementalProcessor
import org.jetbrains.kotlin.kapt.javac.KaptJavaFileObject
import org.jetbrains.kotlin.kapt.stubs.KaptStubConverter
import org.jetbrains.kotlin.kapt.test.handlers.KaptStubConverterHandler.Companion.FILE_SEPARATOR
import org.jetbrains.kotlin.kapt.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt.util.prettyPrint
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import javax.annotation.processing.Completion
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class KaptExtensionProvider(private val testServices: TestServices) : TestService {
    private val cache: MutableMap<TestModule, KaptExtensionForTests> = mutableMapOf()

    fun createExtension(
        module: TestModule,
        kaptOptions: KaptOptions,
        processorOptions: Map<String, String>,
        process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, KaptExtensionForTests) -> Unit,
        supportedAnnotations: List<String>,
        compilerConfiguration: CompilerConfiguration,
    ): KaptExtensionForTests {
        if (module in cache) {
            testServices.assertions.fail { "KaptExtensionForTests for module $module already registered" }
        }

        val extension = KaptExtensionForTests(
            processorOptions,
            kaptOptions,
            process,
            supportedAnnotations,
            compilerConfiguration
        )
        cache[module] = extension
        return extension
    }

    operator fun get(module: TestModule): KaptExtensionForTests {
        return cache.getValue(module)
    }
}

val TestServices.kaptExtensionProvider: KaptExtensionProvider by TestServices.testServiceAccessor()

class KaptExtensionForTests(
    private val processorOptions: Map<String, String>,
    options: KaptOptions,
    private val process: (Set<TypeElement>, RoundEnvironment, ProcessingEnvironment, KaptExtensionForTests) -> Unit,
    val supportedAnnotations: List<String>,
    compilerConfiguration: CompilerConfiguration,
    val messageCollector: MessageCollectorImpl = MessageCollectorImpl()
) : AbstractKaptExtension(
    options, MessageCollectorBackedKaptLogger(
        flags = options,
        messageCollector = messageCollector
    ), compilerConfiguration = compilerConfiguration
) {
    private var _started = false
    val started: Boolean
        get() = _started
    var savedStubs: String? = null
        private set
    var savedBindings: Map<String, KaptJavaFileObject>? = null
        private set

    private val processor = object : Processor {
        lateinit var processingEnv: ProcessingEnvironment

        override fun getSupportedOptions() = processorOptions.keys

        override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
            if (!roundEnv.processingOver()) {
                _started = true
                process(annotations, roundEnv, processingEnv, this@KaptExtensionForTests)
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
        ): Iterable<Completion> {
            return emptyList()
        }

        override fun getSupportedSourceVersion() = SourceVersion.RELEASE_6
        override fun getSupportedAnnotationTypes() = supportedAnnotations.toSet()
    }

    override fun loadProcessors() = LoadedProcessors(
        listOf(IncrementalProcessor(processor, DeclaredProcType.NON_INCREMENTAL, logger)),
        KaptExtensionForTests::class.java.classLoader
    )

    override fun saveStubs(
        kaptContext: KaptContextForStubGeneration,
        stubs: List<KaptStubConverter.KaptStub>,
        messageCollector: MessageCollector,
    ) {
        if (this.savedStubs != null) {
            error("Stubs are already saved")
        }

        this.savedStubs = stubs
            .map { it.file.prettyPrint(kaptContext.context) }
            .sorted()
            .joinToString(FILE_SEPARATOR)

        super.saveStubs(kaptContext, stubs, messageCollector)
    }

    override fun saveIncrementalData(
        kaptContext: KaptContextForStubGeneration,
        messageCollector: MessageCollector,
        converter: KaptStubConverter
    ) {
        if (this.savedBindings != null) {
            error("Bindings are already saved")
        }

        this.savedBindings = converter.bindings

        super.saveIncrementalData(kaptContext, messageCollector, converter)
    }
}
