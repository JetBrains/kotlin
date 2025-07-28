/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.OUTPUT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFir2IrPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelinePhase
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt.base.*
import org.jetbrains.kotlin.kapt.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import org.jetbrains.kotlin.kapt.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt.base.util.info
import org.jetbrains.kotlin.kapt.stubs.KaptStubConverter
import org.jetbrains.kotlin.kapt.stubs.KaptStubConverter.KaptStub
import org.jetbrains.kotlin.kapt.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt.util.prettyPrint
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.utils.kapt.MemoryLeakDetector
import java.io.File

/**
 * This extension implements K2 kapt in the same way as K1 kapt: invoke the compiler in the "skip bodies" / suppress-errors mode,
 * and translate the resulting in-memory class files, correcting error types.
 */
@OptIn(LegacyK2CliPipeline::class)
open class FirKaptAnalysisHandlerExtension(
    private val kaptLogger: MessageCollectorBackedKaptLogger? = null,
) : FirAnalysisHandlerExtension() {
    lateinit var logger: MessageCollectorBackedKaptLogger
    lateinit var options: KaptOptions

    override fun isApplicable(configuration: CompilerConfiguration): Boolean {
        return configuration[KAPT_OPTIONS] != null && configuration.getBoolean(USE_FIR) && !configuration.skipBodies
    }

    override fun doAnalysis(project: Project, configuration: CompilerConfiguration): Boolean {
        val optionsBuilder = configuration[KAPT_OPTIONS]!!
        logger = kaptLogger
            ?: MessageCollectorBackedKaptLogger(
                KaptFlag.VERBOSE in optionsBuilder.flags,
                KaptFlag.INFO_AS_WARNINGS in optionsBuilder.flags,
                configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            )
        val messageCollector = logger.messageCollector

        if (optionsBuilder.mode == AptMode.WITH_COMPILATION) {
            logger.error("KAPT \"compile\" mode is not supported in Kotlin 2.x. Run kapt with -Kapt-mode=stubsAndApt and use kotlinc for the final compilation step.")
            return false
        }

        optionsBuilder.apply {
            projectBaseDir = projectBaseDir ?: project.basePath?.let(::File)
            val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
            compileClasspath.addAll(contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file })
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            classesOutputDir = classesOutputDir ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        }

        optionsBuilder.checkOptions(logger, configuration)?.let { return it }

        options = optionsBuilder.build()
        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        if (options.mode.generateStubs) {
            val updatedConfiguration = configuration.copy().apply {
                this.messageCollector = messageCollector
                skipBodies = true
                useLightTree = false
            }
            val disposable = Disposer.newDisposable("K2KaptSession.project")
            try {
                contextForStubGeneration(disposable, updatedConfiguration)?.use(::generateKotlinSourceStubs)
            } finally {
                disposeRootInWriteAction(disposable)
            }
        }

        if (!options.mode.runAnnotationProcessing) return true

        val processors = loadProcessors()
        if (processors.processors.isEmpty()) return true

        val kaptContext = KaptContext(options, false, logger)

        fun handleKaptError(error: KaptError): Boolean {
            val cause = error.cause

            if (cause != null) {
                kaptContext.logger.exception(cause)
            }

            return false
        }

        try {
            runAnnotationProcessing(kaptContext, processors)
        } catch (error: KaptBaseError) {
            val kind = when (error.kind) {
                KaptBaseError.Kind.EXCEPTION -> KaptError.Kind.EXCEPTION
                KaptBaseError.Kind.ERROR_RAISED -> KaptError.Kind.ERROR_RAISED
            }

            val cause = error.cause
            return handleKaptError(if (cause != null) KaptError(kind, cause) else KaptError(kind))
        } catch (error: KaptError) {
            return handleKaptError(error)
        } catch (thr: Throwable) {
            kaptContext.logger.exception(thr)
            return false
        } finally {
            kaptContext.close()
        }

        return true
    }

    private fun runAnnotationProcessing(kaptContext: KaptContext, processors: LoadedProcessors) {
        if (!options.mode.runAnnotationProcessing) return

        val javaSourceFiles = options.collectJavaSourceFiles(kaptContext.sourcesToReprocess)
        logger.info { "Java source files: " + javaSourceFiles.joinToString { it.normalize().absolutePath } }

        val (annotationProcessingTime) = measureTimeMillis {
            kaptContext.doAnnotationProcessing(javaSourceFiles, processors.processors)
        }

        logger.info { "Annotation processing took $annotationProcessingTime ms" }

        if (options.detectMemoryLeaks != DetectMemoryLeaksMode.NONE) {
            MemoryLeakDetector.add(processors.classLoader)

            val isParanoid = options.detectMemoryLeaks == DetectMemoryLeaksMode.PARANOID
            val (leakDetectionTime, leaks) = measureTimeMillis { MemoryLeakDetector.process(isParanoid) }
            logger.info { "Leak detection took $leakDetectionTime ms" }

            for (leak in leaks) {
                logger.warn(buildString {
                    appendLine("Memory leak detected!")
                    appendLine("Location: '${leak.className}', static field '${leak.fieldName}'")
                    append(leak.description)
                })
            }
        }
    }

    protected open fun updateConfiguration(configuration: CompilerConfiguration) {
    }

    private fun contextForStubGeneration(disposable: Disposable, configuration: CompilerConfiguration): KaptContextForStubGeneration? {
        updateConfiguration(configuration)
        configuration.moduleChunk = ModuleChunk(configuration.modules)

        val frontendInput = ConfigurationPipelineArtifact(
            configuration, DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector), disposable,
        )
        val frontendOutput = JvmFrontendPipelinePhase.executePhase(frontendInput) ?: return null
        configuration.perfManager?.notifyPhaseFinished(PhaseType.Analysis)

        val fir2IrOutput = JvmFir2IrPipelinePhase.executePhase(
            frontendOutput.copy(
                // Ignore all FE errors
                diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector),
            ),
            emptyList(),
        ) ?: return null

        val builderFactory = OriginCollectingClassBuilderFactory(ClassBuilderMode.KAPT3)
        configuration.put(KotlinToJVMBytecodeCompiler.customClassBuilderFactory, builderFactory)
        val backendOutput = JvmBackendPipelinePhase.executePhase(fir2IrOutput) ?: return null
        val generationState = backendOutput.outputs.singleOrNull() ?: return null

        return KaptContextForStubGeneration(
            options, false, logger, builderFactory.compiledClasses, builderFactory.origins, generationState,
            BindingContext.EMPTY, frontendOutput.result.outputs.flatMap { it.fir },
        )
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContextForStubGeneration) {
        val converter = KaptStubConverter(kaptContext, generateNonExistentClass = true)

        val (stubGenerationTime, kaptStubs) = measureTimeMillis {
            converter.convert()
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        logger.info { "Stubs for Kotlin classes: " + kaptStubs.joinToString { it.file.sourcefile.name } }

        saveStubs(kaptContext, kaptStubs, logger.messageCollector)
        saveIncrementalData(kaptContext, logger.messageCollector, converter)
    }

    protected open fun saveStubs(
        kaptContext: KaptContextForStubGeneration,
        stubs: List<KaptStub>,
        messageCollector: MessageCollector,
    ) {
        val reportOutputFiles = kaptContext.generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        val outputFiles = if (reportOutputFiles) kaptContext.generationState.factory.asList().associateBy {
            it.relativePath.substringBeforeLast(".class", missingDelimiterValue = "")
        } else null

        val sourceFiles = mutableListOf<String>()

        for (kaptStub in stubs) {
            val stub = kaptStub.file
            val className = (stub.defs.first { it is JCTree.JCClassDecl } as JCTree.JCClassDecl).simpleName.toString()

            val packageName = stub.getPackageNameJava9Aware()?.toString() ?: ""
            val packageDir =
                if (packageName.isEmpty()) options.stubsOutputDir else File(options.stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val sourceFile = File(packageDir, "$className.java")
            val classFilePathWithoutExtension = if (packageName.isEmpty()) {
                className
            } else {
                "${packageName.replace('.', '/')}/$className"
            }

            sourceFiles += classFilePathWithoutExtension

            fun reportStubsOutputForIC(generatedFile: File) {
                if (!reportOutputFiles) return
                if (classFilePathWithoutExtension == "error/NonExistentClass") return
                val sourceFiles = (outputFiles?.get(classFilePathWithoutExtension)
                    ?: error("The `outputFiles` map is not properly initialized (key = $classFilePathWithoutExtension)")).sourceFiles
                kaptContext.generationState.configuration.fileMappingTracker?.recordSourceFilesToOutputFileMapping(
                    sourceFiles,
                    generatedFile
                )
                messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sourceFiles, generatedFile))
            }

            reportStubsOutputForIC(sourceFile)
            sourceFile.writeText(stub.prettyPrint(kaptContext.context))

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile, ::reportStubsOutputForIC)
        }

        logger.info { "Source files: ${sourceFiles}" }
    }

    protected open fun saveIncrementalData(
        kaptContext: KaptContextForStubGeneration,
        messageCollector: MessageCollector,
        converter: KaptStubConverter,
    ) {
        val incrementalDataOutputDir = options.incrementalDataOutputDir ?: return

        val reportOutputFiles = kaptContext.generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        kaptContext.generationState.factory.writeAll(incrementalDataOutputDir) { outputInfo, output ->
            kaptContext.generationState.configuration.fileMappingTracker?.let {
                it.recordSourceFilesToOutputFileMapping(
                    outputInfo.sourceFiles,
                    output
                )
                if (outputInfo.alwaysDirtyInIncrementalCompilation) {
                    it.recordOutputFileGeneratedForPlugin(output)
                }
            }
            if (reportOutputFiles) {
                messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(outputInfo.sourceFiles, output))
            }
        }
    }

    protected open fun loadProcessors(): LoadedProcessors {
        return EfficientProcessorLoader(options, logger).loadProcessors()
    }

    private fun KaptOptions.Builder.checkOptions(logger: KaptLogger, configuration: CompilerConfiguration): Boolean? {
        if (classesOutputDir == null && configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            logger.error("Kapt does not support specifying JAR file outputs. Please specify the classes output directory explicitly.")
            return false
        }

        if (processingClasspath.isEmpty()) {
            // Skip annotation processing if no annotation processors were provided
            logger.info("No annotation processors provided. Skip KAPT processing.")
            return true
        }

        if (sourcesOutputDir == null || classesOutputDir == null || stubsOutputDir == null) {
            val nonExistentOptionName = when {
                sourcesOutputDir == null -> "Sources output directory"
                classesOutputDir == null -> "Classes output directory"
                else -> "Stubs output directory"
            }
            val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME)
                ?: configuration.get(JVMConfigurationKeys.MODULES).orEmpty().joinToString()

            logger.warn("$nonExistentOptionName is not specified for $moduleName, skipping annotation processing")
            return false
        }

        if (!Kapt.checkJavacComponentsAccess(logger)) {
            return false
        }

        return null
    }
}
