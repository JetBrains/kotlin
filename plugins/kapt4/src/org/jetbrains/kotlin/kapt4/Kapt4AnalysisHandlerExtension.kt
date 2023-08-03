/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.session.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.standalone.KtAlwaysAccessibleLifetimeTokenProvider
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.asJava.findFacadeClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.base.kapt3.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys.USE_FIR
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.kapt3.KAPT_OPTIONS
import org.jetbrains.kotlin.kapt3.base.KaptContext
import org.jetbrains.kotlin.kapt3.base.ProcessorLoader
import org.jetbrains.kotlin.kapt3.base.doAnnotationProcessing
import org.jetbrains.kotlin.kapt3.base.util.KaptLogger
import org.jetbrains.kotlin.kapt3.base.util.getPackageNameJava9Aware
import org.jetbrains.kotlin.kapt3.util.MessageCollectorBackedKaptLogger
import org.jetbrains.kotlin.kapt3.util.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.ServiceLoaderLite
import java.io.File
import java.net.URLClassLoader
import javax.annotation.processing.Processor

private class Kapt4AnalysisHandlerExtension : FirAnalysisHandlerExtension() {
    override fun isApplicable(configuration: CompilerConfiguration): Boolean =
        configuration.getBoolean(USE_FIR) && configuration[KAPT_OPTIONS] != null

    @OptIn(KtAnalysisApiInternals::class)
    override fun doAnalysis(configuration: CompilerConfiguration): Boolean {
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
                @Suppress("DEPRECATION")
                buildKtModuleProviderByCompilerConfiguration(updatedConfiguration)

                registerProjectService(KtLifetimeTokenProvider::class.java, KtAlwaysAccessibleLifetimeTokenProvider())
            }

        val (module, psiFiles) = standaloneAnalysisAPISession.modulesWithFiles.entries.single()
        val ktFiles = psiFiles.filterIsInstance<KtFile>()

        val contentRoots = configuration[CLIConfigurationKeys.CONTENT_ROOTS] ?: emptyList()

        val options = configuration[KAPT_OPTIONS]!!.apply {
            projectBaseDir = projectBaseDir ?: module.project.basePath?.let(::File)
            compileClasspath.addAll(contentRoots.filterIsInstance<JvmClasspathRoot>().map { it.file })
            compileClasspath.addAll(module.directRegularDependencies
                                        .filterIsInstance<KtLibraryModule>()
                                        .flatMap { it.getBinaryRoots() }
                                        .map { it.toFile() })
            javaSourceRoots.addAll(contentRoots.filterIsInstance<JavaSourceRoot>().map { it.file })
            classesOutputDir = classesOutputDir ?: configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        }.build()

        val logger = MessageCollectorBackedKaptLogger(
            options.flags[KaptFlag.VERBOSE],
            options.flags[KaptFlag.INFO_AS_WARNINGS],
            configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)!!
        )

        if (options[KaptFlag.VERBOSE]) {
            logger.info(options.logString())
        }

        if (options.mode == AptMode.WITH_COMPILATION) {
            logger.error("KAPT \"compile\" mode is not supported in Kotlin 2.x. Run kapt with -Kapt-mode=stubsAndApt and use kotlinc for the final compilation step.")
            return false
        }

        return try {
            val lightClasses = buildSet {
                ktFiles.flatMapTo(this) { file ->
                    file.children.filterIsInstance<KtClassOrObject>().mapNotNull {
                        it.toLightClass()
                    }
                }
                ktFiles.mapNotNullTo(this) { ktFile -> ktFile.findFacadeClass() }.distinct()
            }

            KtAnalysisSessionProvider.getInstance(module.project).analyze(module) {
                val context = Kapt4ContextForStubGeneration(
                    options,
                    withJdk = false,
                    logger,
                    this,
                    lightClasses
                )

                if (options.mode != AptMode.APT_ONLY)
                    generateStubs(context)

                if (options.mode != AptMode.STUBS_ONLY)
                    runProcessors(options, options.collectJavaSourceFiles(context.sourcesToReprocess), logger)
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
        File(File(context.options.stubsOutputDir, "error").apply { mkdirs() }, "NonExistentClass.java")
            .writeText("package error;\npublic class NonExistentClass {}\n")
    }

    private fun runProcessors(
        options: KaptOptions,
        sources: List<File>,
        logger: KaptLogger,
    ) {
        var sourcesToProcess = sources
        var processedSources = emptySet<File>()
        val processorLoader = object : ProcessorLoader(options, logger) {
            override fun doLoadProcessors(classpath: LinkedHashSet<File>, classLoader: ClassLoader): List<Processor> =
                when (classLoader) {
                    is URLClassLoader -> ServiceLoaderLite.loadImplementations(Processor::class.java, classLoader)
                    else -> super.doLoadProcessors(classpath, classLoader)
                }
        }

        val loaderWithJavac = Context::class.java.classLoader ?: ClassLoader.getSystemClassLoader()
        while (sourcesToProcess.isNotEmpty()) {
            val processingContext = KaptContext(
                options,
                withJdk = false,
                logger
            )
            val processors = processorLoader.loadProcessors(loaderWithJavac)
            processingContext.doAnnotationProcessing(
                sourcesToProcess,
                processors.processors,
            )
            processedSources += sourcesToProcess
            sourcesToProcess = options.sourcesOutputDir.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".java", true) && it !in processedSources }
                .toList()
        }
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