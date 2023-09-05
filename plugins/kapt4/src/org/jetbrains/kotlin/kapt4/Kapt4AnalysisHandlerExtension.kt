/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.base.kapt3.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt3.EfficientProcessorLoader
import org.jetbrains.kotlin.kapt3.KAPT_OPTIONS
import org.jetbrains.kotlin.kapt3.base.Kapt
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt3.util.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

private class Kapt4AnalysisHandlerExtension : FirAnalysisHandlerExtension() {
    override fun isApplicable(configuration: CompilerConfiguration): Boolean {
        val options = configuration[KAPT_OPTIONS]
        return options != null && (configuration.getBoolean(USE_FIR) || KaptFlag.USE_K2 in options.flags)
    }

    @OptIn(KtAnalysisApiInternals::class)
    override fun doAnalysis(configuration: CompilerConfiguration): Boolean {
        val optionsBuilder = configuration[KAPT_OPTIONS]!!
        val logger = MessageCollectorBackedKaptLogger(
            KaptFlag.VERBOSE in optionsBuilder.flags,
            KaptFlag.INFO_AS_WARNINGS in optionsBuilder.flags,
            configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)!!
        )

        if (optionsBuilder.mode == AptMode.WITH_COMPILATION) {
            logger.error("KAPT \"compile\" mode is not supported in Kotlin 2.x. Run kapt with -Kapt-mode=stubsAndApt and use kotlinc for the final compilation step.")
            return false
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

        val standaloneAnalysisAPISession =
            buildStandaloneAnalysisAPISession(classLoader = Kapt4AnalysisHandlerExtension::class.java.classLoader) {
                @Suppress("DEPRECATION") // TODO: KT-61319 Kapt: remove usages of deprecated buildKtModuleProviderByCompilerConfiguration
                buildKtModuleProviderByCompilerConfiguration(updatedConfiguration)

                registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())
            }

        val (module, psiFiles) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()

        optionsBuilder.apply {
            projectBaseDir = projectBaseDir ?: module.project.basePath?.let(::File)
            val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()
            compileClasspath.addAll(contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file })
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            classesOutputDir = classesOutputDir ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        }

        if (!optionsBuilder.checkOptions(logger, configuration)) return false
        val options = optionsBuilder.build()
        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        return try {
            KtAnalysisSessionProvider.getInstance(module.project).analyze(module) {
                Kapt4ContextForStubGeneration(
                    options,
                    false,
                    logger,
                    this,
                    psiFiles.filterIsInstance<KtFile>(),
                    configuration[CommonConfigurationKeys.METADATA_VERSION]
                ).use { context ->
                    if (options.mode.generateStubs)
                        generateStubs(context)

                    if (options.mode.runAnnotationProcessing)
                        runProcessors(context, options, logger)
                }
                true
            }
        } catch (e: Exception) {
            logger.exception(e)
            false
        }
    }

    private fun generateStubs(context: Kapt4ContextForStubGeneration) {
        val generator = with(context) { Kapt4StubGenerator() }
        val stubs = generator.generateStubs().values.filterNotNull().toList()
        for (kaptStub in stubs) {
            val stub = kaptStub.file
            val className = (stub.defs.first { it is JCTree.JCClassDecl } as JCTree.JCClassDecl).simpleName.toString()

            val packageName = stub.getPackageNameJava9Aware()?.toString() ?: ""
            val stubsOutputDir = context.options.stubsOutputDir
            val packageDir = if (packageName.isEmpty()) stubsOutputDir else File(stubsOutputDir, packageName.replace('.', '/'))
            packageDir.mkdirs()

            val sourceFile = File(packageDir, "$className.java")
            sourceFile.writeText(stub.prettyPrint(context.context))

            kaptStub.writeMetadataIfNeeded(forSource = sourceFile)
        }
        File(context.options.stubsOutputDir, "error").apply { mkdirs() }.resolve("NonExistentClass.java")
            .writeText("package error;\npublic class NonExistentClass {}\n")
    }

    private fun runProcessors(
        context: Kapt4ContextForStubGeneration,
        options: KaptOptions,
        logger: KaptLogger,
    ) {
        val sources = options.collectJavaSourceFiles(context.sourcesToReprocess)
        if (sources.isEmpty()) return
        EfficientProcessorLoader(options, logger).use {
            context.doAnnotationProcessing(sources, it.loadProcessors().processors)
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
        Companion.registerExtensions(this)
    }

    override val supportsK2: Boolean
        get() = true


    companion object {
        fun registerExtensions(extensionStorage: ExtensionStorage) = with(extensionStorage) {
            FirAnalysisHandlerExtension.registerExtension(Kapt4AnalysisHandlerExtension())
        }
    }
}
