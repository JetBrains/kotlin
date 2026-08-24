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
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.modules.ModuleChunk
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.registerExtensionStorage
import org.jetbrains.kotlin.cli.reportOutput
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt.base.*
import org.jetbrains.kotlin.kapt.base.util.KaptBaseError
import org.jetbrains.kotlin.kapt.base.util.KaptLogger
import org.jetbrains.kotlin.kapt.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt.base.util.info
import org.jetbrains.kotlin.kapt.stubs.KaptStubConverter
import org.jetbrains.kotlin.kapt.stubs.KaptStubConverter.KaptStub
import org.jetbrains.kotlin.kapt.util.CompilerConfigurationBackedKaptLogger
import org.jetbrains.kotlin.kapt.util.prettyPrint
import org.jetbrains.kotlin.kapt3.diagnostic.KaptError
import org.jetbrains.kotlin.utils.kapt.MemoryLeakDetector
import java.io.File

/**
 * This extension implements K2 kapt by invoking the compiler in the "skip bodies" / suppress-errors mode, and translating the resulting
 * in-memory class files to Java sources, correcting error types.
 */
open class FirKaptAnalysisHandlerExtension(
    private val kaptLogger: CompilerConfigurationBackedKaptLogger? = null,
) : FirAnalysisHandlerExtension() {
    lateinit var logger: CompilerConfigurationBackedKaptLogger
    lateinit var options: KaptOptions

    override fun isApplicable(configuration: CompilerConfiguration): Boolean {
        return configuration[KAPT_OPTIONS] != null && !configuration.skipBodies
    }

    override fun doAnalysis(project: Project, configuration: CompilerConfiguration): Boolean {
        val optionsBuilder = configuration[KAPT_OPTIONS]!!
        logger = kaptLogger ?: CompilerConfigurationBackedKaptLogger(
            KaptFlag.VERBOSE in optionsBuilder.flags,
            KaptFlag.INFO_AS_WARNINGS in optionsBuilder.flags,
            configuration,
        )

        if (optionsBuilder.mode == AptMode.WITH_COMPILATION) {
            logger.error("KAPT \"compile\" mode is not supported in Kotlin 2.x. Run kapt with -Kapt-mode=stubsAndApt and use kotlinc for the final compilation step.")
            return false
        }

        optionsBuilder.apply {
            projectBaseDir = projectBaseDir ?: project.basePath?.let(::File)
            val contentRoots = configuration.contentRoots
            compileClasspath.addAll(contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file })
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            classesOutputDir = classesOutputDir ?: configuration.outputDirectory
        }

        optionsBuilder.checkOptions(logger, configuration)?.let { return it }

        options = optionsBuilder.build()
        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        if (options.mode.generateStubs) {
            val updatedConfiguration = configuration.copy().apply {
                skipBodies = true
                useLightTree = false

                /*
                 * Later the KAPT pipeline registers extensions once again, so the extensions storage
                 * should be reset. Otherwise the extensions would be duplicated.
                 */
                @OptIn(CompilerConfiguration.Internals::class)
                registerExtensionStorage()
            }
            val disposable = Disposer.newDisposable("K2KaptSession.project")
            try {
                contextForStubGeneration(disposable, updatedConfiguration)?.use(::generateKotlinSourceStubs)
            } finally {
                disposeRootInWriteAction(disposable)
            }
        }

        if (!options.mode.runAnnotationProcessing) return true

        createProcessorLoader().use { processorLoader ->
            val processors = processorLoader.loadProcessors()
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
        }

        return true
    }

    private fun runAnnotationProcessing(kaptContext: KaptContext, processors: LoadedProcessors) {
        if (!options.mode.runAnnotationProcessing) return

        val javaSourceFiles = options.collectJavaSourceFiles(kaptContext.sourcesToReprocess)
        logger.info { "Java source files: " + javaSourceFiles.joinToString { it.normalize().absolutePath } }

        val [annotationProcessingTime] = measureTimeMillis {
            kaptContext.doAnnotationProcessing(javaSourceFiles, processors.processors)
        }

        logger.info { "Annotation processing took $annotationProcessingTime ms" }

        if (options.detectMemoryLeaks != DetectMemoryLeaksMode.NONE) {
            MemoryLeakDetector.add(processors.classLoader)

            val isParanoid = options.detectMemoryLeaks == DetectMemoryLeaksMode.PARANOID
            val [leakDetectionTime, leaks] = measureTimeMillis { MemoryLeakDetector.process(isParanoid) }
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

    private fun contextForStubGeneration(disposable: Disposable, configuration: CompilerConfiguration): KaptContextForStubGeneration? {
        configuration.moduleChunk = ModuleChunk(configuration.modules)

        return compileForStubGeneration(
            disposable,
            configuration,
            options,
            logger,
            withJdk = false,
            onFrontendOutput = ::checkForSyntaxErrorsAndReport,
        )
    }

    private fun checkForSyntaxErrorsAndReport(frontendOutput: JvmFrontendPipelineArtifact): Boolean {
        var reported = false
        FirDiagnosticsCompilerResultsReporter.reportByFile(frontendOutput.configuration.diagnosticsCollector) { diagnostic, location ->
            if (diagnostic.factory == FirSyntaxErrors.SYNTAX) {
                FirDiagnosticsCompilerResultsReporter.reportDiagnosticToConfiguration(
                    diagnostic, location, logger.configuration, frontendOutput.configuration.renderDiagnosticInternalName
                )
                reported = true
            }
        }
        return reported
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContextForStubGeneration) {
        val converter = KaptStubConverter(kaptContext, generateNonExistentClass = true)

        val [stubGenerationTime, kaptStubs] = measureTimeMillis {
            converter.convert()
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        logger.info {
            "Stubs for Kotlin classes: " + kaptStubs.joinToString {
                if (options.stubGenerationScheme == StubGenerationScheme.DIRECT)
                    it.directClassFilePathWithoutExtension + ".java"
                else
                    it.jtreeFile.sourcefile.name
            }
        }

        saveStubs(kaptContext, kaptStubs)
        saveIncrementalData(kaptContext, converter)
    }

    protected open fun saveStubs(
        kaptContext: KaptContextForStubGeneration,
        stubs: List<KaptStub>,
    ) {
        val reportOutputFiles = kaptContext.configuration.reportOutputFiles
        val outputFiles = if (reportOutputFiles) kaptContext.classFileFactory.asList().associateBy {
            it.relativePath.substringBeforeLast(".class", missingDelimiterValue = "")
        } else null

        val sourceFiles = mutableListOf<String>()

        for (kaptStub in stubs) {
            val stubFile = kaptStub.jtreeFile
            val className: String
            val packageName: String
            val classFilePathWithoutExtension: String
            if (options.stubGenerationScheme == StubGenerationScheme.DIRECT) {
                className = kaptStub.directSimpleClassName
                packageName = kaptStub.directPackageName
                classFilePathWithoutExtension = kaptStub.directClassFilePathWithoutExtension
            } else {
                className = (stubFile.defs.first { it is JCTree.JCClassDecl } as JCTree.JCClassDecl).simpleName.toString()
                packageName = stubFile.getPackageNameJava9Aware()?.toString() ?: ""
                classFilePathWithoutExtension = if (packageName.isEmpty()) {
                    className
                } else {
                    "${packageName.replace('.', '/')}/$className"
                }
            }

            val packageDir =
                if (packageName.isEmpty()) options.stubsOutputDir else File(options.stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val sourceFile = File(packageDir, "$className.java")

            sourceFiles += classFilePathWithoutExtension

            fun reportStubsOutputForIC(generatedFile: File) {
                if (!reportOutputFiles) return
                if (classFilePathWithoutExtension == "error/NonExistentClass") return
                val sourceFiles = (outputFiles?.get(classFilePathWithoutExtension)
                    ?: error("The `outputFiles` map is not properly initialized (key = $classFilePathWithoutExtension)")).sourceFiles
                kaptContext.configuration.fileMappingTracker?.recordSourceFilesToOutputFileMapping(sourceFiles, generatedFile)
                logger.configuration.reportOutput(OutputMessageUtil.formatOutputMessage(sourceFiles, generatedFile))
            }

            reportStubsOutputForIC(sourceFile)
            sourceFile.writeText(
                if (options.stubGenerationScheme == StubGenerationScheme.DIRECT)
                    kaptStub.directFileContent
                else
                    kaptStub.jtreeFile.prettyPrint(kaptContext.context)
            )

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile, ::reportStubsOutputForIC)
        }

        logger.info { "Source files: ${sourceFiles}" }
    }

    protected open fun saveIncrementalData(
        kaptContext: KaptContextForStubGeneration,
        converter: KaptStubConverter,
    ) {
        val incrementalDataOutputDir = options.incrementalDataOutputDir ?: return

        val reportOutputFiles = kaptContext.configuration.reportOutputFiles
        kaptContext.classFileFactory.writeAll(incrementalDataOutputDir) { outputInfo, output ->
            kaptContext.configuration.fileMappingTracker?.let {
                when (outputInfo.generatedForCompilerPlugin) {
                    false -> it.recordSourceFilesToOutputFileMapping(
                        outputInfo.sourceFiles,
                        output
                    )

                    true -> it.recordOutputFileGeneratedForPlugin(output)
                }
            }
            if (reportOutputFiles) {
                logger.configuration.reportOutput(OutputMessageUtil.formatOutputMessage(outputInfo.sourceFiles, output))
            }
        }
    }

    protected open fun createProcessorLoader(): ProcessorLoader =
        EfficientProcessorLoader(options, logger)

    private fun KaptOptions.Builder.checkOptions(logger: KaptLogger, configuration: CompilerConfiguration): Boolean? {
        if (classesOutputDir == null && configuration.outputJar != null) {
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
            val moduleName = configuration.moduleName ?: configuration.modules.joinToString()
            logger.warn("$nonExistentOptionName is not specified for $moduleName, skipping annotation processing")
            return false
        }

        if (!Kapt.checkJavacComponentsAccess(logger)) {
            return false
        }

        return null
    }

    private inline fun <T> measureTimeMillis(block: () -> T): Pair<Long, T> {
        val start = System.currentTimeMillis()
        val result = block()
        return Pair(System.currentTimeMillis() - start, result)
    }
}
