/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.GroupedKtSources
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.OUTPUT
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil.formatOutputMessage
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.FirKotlinToJvmBytecodeCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.pipeline.*
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.OriginCollectingClassBuilderFactory
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt3.EfficientProcessorLoader
import org.jetbrains.kotlin.kapt3.KAPT_OPTIONS
import org.jetbrains.kotlin.kapt3.KaptContextForStubGeneration
import org.jetbrains.kotlin.kapt3.base.*
import org.jetbrains.kotlin.kapt3.base.util.*
import org.jetbrains.kotlin.kapt3.measureTimeMillis
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter
import org.jetbrains.kotlin.kapt3.stubs.ClassFileToSourceStubConverter.KaptStub
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt3.util.prettyPrint
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File

private class Kapt4AnalysisHandlerExtension : FirAnalysisHandlerExtension() {
    lateinit var logger: MessageCollectorBackedKaptLogger
    lateinit var options: KaptOptions

    override fun isApplicable(configuration: CompilerConfiguration): Boolean {
        return configuration[KAPT_OPTIONS] != null && configuration.getBoolean(USE_FIR)
    }

    override fun doAnalysis(project: Project, module: Module, configuration: CompilerConfiguration): Boolean {
        val optionsBuilder = configuration[KAPT_OPTIONS]!!
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        logger = MessageCollectorBackedKaptLogger(
            KaptFlag.VERBOSE in optionsBuilder.flags,
            KaptFlag.INFO_AS_WARNINGS in optionsBuilder.flags,
            messageCollector
        )

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

        if (!optionsBuilder.checkOptions(logger, configuration)) return false
        options = optionsBuilder.build()
        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        val oldLanguageVersionSettings = configuration.languageVersionSettings
        val updatedConfiguration = configuration.copy().apply {
            languageVersionSettings = object : LanguageVersionSettings by oldLanguageVersionSettings {
                @Suppress("UNCHECKED_CAST")
                override fun <T> getFlag(flag: AnalysisFlag<T>): T =
                    when (flag) {
                        JvmAnalysisFlags.generatePropertyAnnotationsMethods -> true as T
                        else -> oldLanguageVersionSettings.getFlag(flag)
                    }
            }
        }

        val groupedSources: GroupedKtSources = collectSources(updatedConfiguration, project, messageCollector)
        if (messageCollector.hasErrors()) {
            return false
        }

        val sourceFiles = groupedSources.commonSources + groupedSources.platformSources
        logger.info { "Kotlin files to compile: ${sourceFiles.map { it.name }}" }

        val compilerInput = ModuleCompilerInput(
            TargetId(module),
            groupedSources,
            CommonPlatforms.defaultCommonPlatform,
            JvmPlatforms.unspecifiedJvmPlatform,
            updatedConfiguration
        )

        val projectDisposable = Disposer.newDisposable("K2KaptSession.project")
        try {
            val projectEnvironment =
                createProjectEnvironment(updatedConfiguration, projectDisposable, EnvironmentConfigFiles.JVM_CONFIG_FILES, messageCollector)
            if (messageCollector.hasErrors()) {
                return false
            }

            val diagnosticsReporter = FirKotlinToJvmBytecodeCompiler.createPendingReporter(messageCollector)

            if (options.mode.generateStubs) {
                val (analysisTime, analysisResults) = measureTimeMillis {
                    compileModuleToAnalyzedFir(
                        compilerInput,
                        projectEnvironment,
                        emptyList(),
                        null,
                        diagnosticsReporter,
                    )
                }

                logger.info { "Initial analysis took $analysisTime ms" }

                val (classFilesCompilationTime, codegenOutput) = measureTimeMillis {
                    // Ignore all FE errors
                    val cleanDiagnosticReporter = FirKotlinToJvmBytecodeCompiler.createPendingReporter(messageCollector)
                    val compilerEnvironment = ModuleCompilerEnvironment(projectEnvironment, cleanDiagnosticReporter)
                    val irInput = convertAnalyzedFirToIr(compilerInput, analysisResults, compilerEnvironment, skipBodies = true)

                    generateCodeFromIr(irInput, compilerEnvironment, skipBodies = true)
                }

                val builderFactory = codegenOutput.builderFactory
                val compiledClasses = (builderFactory as OriginCollectingClassBuilderFactory).compiledClasses
                val origins = builderFactory.origins

                logger.info { "Stubs compilation took $classFilesCompilationTime ms" }
                logger.info { "Compiled classes: " + compiledClasses.joinToString { it.name } }

                KaptContextForStubGeneration(
                    options, false, logger, compiledClasses, origins, codegenOutput.generationState,
                    analysisResults.outputs.flatMap { it.fir }
                ).use { context ->
                    generateKotlinSourceStubs(context)
                }
            }

            if (options.mode.runAnnotationProcessing) {
                val (annotationProcessingTime) = measureTimeMillis {
                    KaptContext(
                        options,
                        false,
                        logger
                    ).use { context ->
                        try {
                            runProcessors(context, options)
                        } catch (e: KaptBaseError) {
                            return false
                        }
                    }
                }

                logger.info { "Annotation processing took $annotationProcessingTime ms" }
            }

            return true

//        val projectDisposable = Disposer.newDisposable("StandaloneAnalysisAPISession.project")
//        try {
//            val standaloneAnalysisAPISession =
//                buildStandaloneAnalysisAPISession(
//                    projectDisposable = projectDisposable,
//                    classLoader = Kapt4AnalysisHandlerExtension::class.java.classLoader) {
//                    @Suppress("DEPRECATION") // TODO: KT-61319 Kapt: remove usages of deprecated buildKtModuleProviderByCompilerConfiguration
//                    buildKtModuleProviderByCompilerConfiguration(updatedConfiguration)
//
//                    registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())
//                    registerProjectService(KtCompilerPluginsProvider::class.java, object : KtCompilerPluginsProvider() {
//                        private val extensionStorage = CompilerPluginRegistrar.ExtensionStorage().apply {
//                            for (registrar in updatedConfiguration.getList(CompilerPluginRegistrar.COMPILER_PLUGIN_REGISTRARS)) {
//                                with(registrar) { registerExtensions(updatedConfiguration) }
//                            }
//                        }
//
//                        override fun <T : Any> getRegisteredExtensions(
//                            module: KtSourceModule,
//                            extensionType: ProjectExtensionDescriptor<T>,
//                        ): List<T> {
//                            @Suppress("UNCHECKED_CAST")
//                            return (extensionStorage.registeredExtensions[extensionType] as? List<T>) ?: emptyList()
//                        }
//
//                        override fun isPluginOfTypeRegistered(module: KtSourceModule, pluginType: CompilerPluginType): Boolean = false
//                    })
//                }
//
//            val (module, files) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()
//
//            optionsBuilder.apply {
//                projectBaseDir = projectBaseDir ?: module.project.basePath?.let(::File)
//                val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
//                compileClasspath.addAll(contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file })
//                javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
//                classesOutputDir = classesOutputDir ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
//            }
//
//            if (!optionsBuilder.checkOptions(logger, configuration)) return false
//            val options = optionsBuilder.build()
//            if (options[KaptFlag.VERBOSE]) {
//                logger.info(options.logString())
//            }
//
//            return try {
//                if (options.mode.generateStubs) {
//                    generateAndSaveStubs(
//                        module,
//                        files,
//                        options,
//                        logger,
//                        configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES),
//                        configuration.metadataVersion()
//                    )
//
//                }
//                if (options.mode.runAnnotationProcessing) {
//                    KaptContext(
//                        options,
//                        false,
//                        logger
//                    ).use { context ->
//                        try {
//                            runProcessors(context, options)
//                        } catch (e: KaptBaseError) {
//                            return false
//                        }
//                    }
//                }
//                true
//            } catch (e: Exception) {
//                logger.exception(e)
//                false
//            }
        } finally {
            Disposer.dispose(projectDisposable)
        }
    }

    private fun generateKotlinSourceStubs(kaptContext: KaptContextForStubGeneration) {
        val converter = ClassFileToSourceStubConverter(kaptContext, generateNonExistentClass = true)

        val (stubGenerationTime, kaptStubs) = measureTimeMillis {
            converter.convert()
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        logger.info { "Stubs for Kotlin classes: " + kaptStubs.joinToString { it.file.sourcefile.name } }

        saveStubs(kaptContext, kaptStubs, logger.messageCollector)
        saveIncrementalData(kaptContext, logger.messageCollector)
    }

    private fun saveStubs(
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
                messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sourceFiles, generatedFile))
            }

            reportStubsOutputForIC(sourceFile)
            sourceFile.writeText(stub.prettyPrint(kaptContext.context))

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile, ::reportStubsOutputForIC)
        }

        logger.info { "Source files: ${sourceFiles}" }
    }

    private fun saveIncrementalData(
        kaptContext: KaptContextForStubGeneration,
        messageCollector: MessageCollector
    ) {
        val incrementalDataOutputDir = options.incrementalDataOutputDir ?: return

        val reportOutputFiles = kaptContext.generationState.configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
        kaptContext.generationState.factory.writeAll(
            incrementalDataOutputDir,
            if (!reportOutputFiles) null else fun(sources: List<File>, output: File) {
                messageCollector.report(OUTPUT, OutputMessageUtil.formatOutputMessage(sources, output))
            }
        )
    }

    private fun generateAndSaveStubs(
        module: KtSourceModule,
        files: List<PsiFile>,
        options: KaptOptions,
        logger: MessageCollectorBackedKaptLogger,
        reportOutputFiles: Boolean,
        metadataVersion: BinaryVersion
    ) {
        val (stubGenerationTime, classesToStubs) = measureTimeMillis {
            generateStubs(module, files, options, logger, metadataVersion)
        }

        logger.info { "Java stub generation took $stubGenerationTime ms" }
        val infoBuilder = if (logger.isVerbose) StringBuilder("Stubs for Kotlin classes: ") else null

        for ((lightClass, kaptStub) in classesToStubs) {
            if (kaptStub == null) continue
            val stub = kaptStub.source
            val className = lightClass.name
            val packageName = (lightClass.parent as PsiJavaFile).packageName
            val stubsOutputDir = options.stubsOutputDir
            val packageDir = if (packageName.isEmpty()) stubsOutputDir else File(stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val generatedFile = File(packageDir, "$className.java")
            generatedFile.writeText(stub)
            infoBuilder?.append(generatedFile.path)
            kaptStub.writeMetadata(forSource = generatedFile)

            if (reportOutputFiles) {
                val ktFiles = when(lightClass) {
                    is KtLightClassForFacade -> lightClass.files
                    else -> listOfNotNull(lightClass.kotlinOrigin?.containingKtFile)
                }
                val report = formatOutputMessage(ktFiles.map { it.virtualFilePath }, generatedFile.path)
                logger.messageCollector.report(CompilerMessageSeverity.OUTPUT, report)
            }
        }

        logger.info { infoBuilder.toString() }

        File(options.stubsOutputDir, "error").apply { mkdirs() }.resolve("NonExistentClass.java")
            .writeText("package error;\npublic class NonExistentClass {}\n")
    }

    private fun runProcessors(context: KaptContext, options: KaptOptions) {
        val sources = options.collectJavaSourceFiles(context.sourcesToReprocess)
        if (sources.isEmpty()) return
        EfficientProcessorLoader(options, context.logger).use {
            val processors = it.loadProcessors().processors
            if (processors.isEmpty()) return
            context.doAnnotationProcessing(sources, processors)
        }
    }

    private fun KaptOptions.Builder.checkOptions(logger: KaptLogger, configuration: CompilerConfiguration): Boolean {
        if (classesOutputDir == null && configuration.get(JVMConfigurationKeys.OUTPUT_JAR) != null) {
            logger.error("Kapt does not support specifying JAR file outputs. Please specify the classes output directory explicitly.")
            return false
        }

        if (processingClasspath.isEmpty()) {
            // Skip annotation processing if no annotation processors were provided
            logger.info("No annotation processors provided. Skip KAPT processing.")
            return false
        }

        if (sourcesOutputDir == null || classesOutputDir == null || stubsOutputDir == null) {
            if (mode != AptMode.WITH_COMPILATION) {
                val nonExistentOptionName = when {
                    sourcesOutputDir == null -> "Sources output directory"
                    classesOutputDir == null -> "Classes output directory"
                    stubsOutputDir == null -> "Stubs output directory"
                    else -> throw IllegalStateException()
                }
                val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME)
                    ?: configuration.get(JVMConfigurationKeys.MODULES).orEmpty().joinToString()

                logger.warn("$nonExistentOptionName is not specified for $moduleName, skipping annotation processing")
            }
            return false
        }

        if (!Kapt.checkJavacComponentsAccess(logger)) {
            return false
        }

        return true
    }
}

class Kapt4CompilerPluginRegistrar : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (!configuration.getBoolean(USE_FIR)) return

        doOpenInternalPackagesIfRequired()

        FirAnalysisHandlerExtension.registerExtension(Kapt4AnalysisHandlerExtension())
    }

    override val supportsK2: Boolean
        get() = true
}
