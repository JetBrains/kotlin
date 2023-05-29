/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.KotlinPaths
import java.util.*


private class K2NativeCompilerPerformanceManager: CommonCompilerPerformanceManager("Kotlin to Native Compiler")

class K2Native : CLICompiler<K2NativeCompilerArguments>() {

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeCompilerArguments) {}

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = KlibMetadataVersion(*versionArray)

    override val defaultPerformanceManager: CommonCompilerPerformanceManager by lazy {
        K2NativeCompilerPerformanceManager()
    }

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${KotlinCompilerVersion.getVersion() ?: "SNAPSHOT"}")
            return ExitCode.OK
        }

        val pluginLoadResult =
                PluginCliParser.loadPluginsSafe(arguments.pluginClasspaths, arguments.pluginOptions, arguments.pluginConfigurations, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE

        val enoughArguments = arguments.freeArgs.isNotEmpty() || arguments.isUsefulWithoutFreeArgs
        if (!enoughArguments) {
            messageCollector.report(ERROR, "You have not specified any compilation arguments. No output has been produced.")
        }
        if(configuration.get(KonanConfigKeys.PRODUCE) != CompilerOutputKind.LIBRARY &&
                configuration.getBoolean(CommonConfigurationKeys.USE_FIR) &&
                configuration.kotlinSourceRoots.isNotEmpty()) {
            // K2/Native backend cannot produce binary directly from FIR frontend output, since descriptors, deserialized from KLib, are needed
            // So, such compilation is split to two stages:
            // - source files are compiled to intermediate KLib by FIR frontend
            // - intermediate Klib is compiled to binary by K2/Native backend
            // In this implementation, 'arguments' is not changed accordingly to changes in `firstStageConfiguration` and `configuration`,
            // since values of fields `produce`, `output`, `freeArgs`, `includes` does not seem to matter downstream in prepareEnvironment()

            val firstStageConfiguration = configuration.copy()
            // For the first stage, use "-p library" produce mode
            firstStageConfiguration.put(KonanConfigKeys.PRODUCE, CompilerOutputKind.LIBRARY)
            // For the first stage, construct a temporary file name for an intermediate KLib
            val intermediateKLib = File(System.getProperty("java.io.tmpdir"), "${UUID.randomUUID()}.klib").also {
                require(!it.exists) { "Collision writing intermediate KLib $it"}
                it.deleteOnExit()
            }
            firstStageConfiguration.put(KonanConfigKeys.OUTPUT, intermediateKLib.absolutePath)

            val firstStageExitCode = executeStage(firstStageConfiguration, arguments, rootDisposable)
            if (firstStageExitCode != ExitCode.OK)
                return firstStageExitCode

            // For the second stage, remove already compiled source files from the configuration
            configuration.put(CLIConfigurationKeys.CONTENT_ROOTS, listOf())
            // For the second stage, provide just compiled intermediate KLib as "-Xinclude=" param
            require(intermediateKLib.exists) { "Intermediate KLib $intermediateKLib must have been created by successful first compilation stage" }
            configuration.put(KonanConfigKeys.INCLUDED_LIBRARIES, listOf(intermediateKLib.absolutePath))
            // Now, `configuration` param is prepared for the second stage of compilation, and `arguments` param does not need changes, as noted above.
        }
        return executeStage(configuration, arguments, rootDisposable)
    }

    private fun executeStage(
            configuration: CompilerConfiguration,
            arguments: K2NativeCompilerArguments,
            rootDisposable: Disposable
    ): ExitCode {
        val environment = prepareEnvironment(arguments, configuration, rootDisposable)

        try {
            runKonanDriver(configuration, environment, rootDisposable)
        } catch (e: Throwable) {
            if (e is KonanCompilationException || e is CompilationErrorException)
                return ExitCode.COMPILATION_ERROR

            configuration.report(ERROR, """
                |Compilation failed: ${e.message}

                | * Source files: ${environment.getSourceFiles().joinToString(transform = KtFile::getName)}
                | * Compiler version: ${KotlinCompilerVersion.getVersion()}
                | * Output kind: ${configuration.get(KonanConfigKeys.PRODUCE)}

                """.trimMargin())
            throw e
        }

        return ExitCode.OK
    }

    private fun prepareEnvironment(
            arguments: K2NativeCompilerArguments,
            configuration: CompilerConfiguration,
            rootDisposable: Disposable
    ): KotlinCoreEnvironment {
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
                configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)

        configuration.put(CLIConfigurationKeys.FLEXIBLE_PHASE_CONFIG, createFlexiblePhaseConfig(arguments))

        /* Set default version of metadata version */
        val metadataVersionString = arguments.metadataVersion
        if (metadataVersionString == null) {
            configuration.put(CommonConfigurationKeys.METADATA_VERSION, KlibMetadataVersion.INSTANCE)
        }

        val relativePathBases = arguments.relativePathBases
        if (relativePathBases != null) {
            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, relativePathBases.toList())
        }

        configuration.put(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, arguments.normalizeAbsolutePath)
        configuration.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)

        return environment
    }

    private fun runKonanDriver(
            configuration: CompilerConfiguration,
            environment: KotlinCoreEnvironment,
            rootDisposable: Disposable
    ) {
        val konanDriver = KonanDriver(environment.project, environment, configuration) { args, setupConfiguration ->
            val spawnedArguments = K2NativeCompilerArguments()
            parseCommandLineArguments(args, spawnedArguments)
            val spawnedConfiguration = CompilerConfiguration()

            spawnedConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY))
            spawnedConfiguration.put(IrMessageLogger.IR_MESSAGE_LOGGER, configuration.getNotNull(IrMessageLogger.IR_MESSAGE_LOGGER))
            spawnedConfiguration.setupCommonArguments(spawnedArguments, this::createMetadataVersion)
            spawnedConfiguration.setupFromArguments(spawnedArguments)
            spawnedConfiguration.setupPartialLinkageConfig(configuration.partialLinkageConfig)
            spawnedConfiguration.setupConfiguration()
            val spawnedEnvironment = prepareEnvironment(spawnedArguments, spawnedConfiguration, rootDisposable)
            runKonanDriver(spawnedConfiguration, spawnedEnvironment, rootDisposable)
        }
        konanDriver.run()
    }

    private val K2NativeCompilerArguments.isUsefulWithoutFreeArgs: Boolean
        get() = listTargets || listPhases || checkDependencies || !includes.isNullOrEmpty() ||
                libraryToAddToCache != null || !exportedLibraries.isNullOrEmpty() || !compileFromBitcode.isNullOrEmpty()

    // It is executed before doExecute().
    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments: K2NativeCompilerArguments,
            services: Services
    ) {
        configuration.setupFromArguments(arguments)
    }

    override fun createArguments() = K2NativeCompilerArguments()

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                doMain(K2Native(), args)
            }
        }

        @JvmStatic fun mainNoExit(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }

        @JvmStatic fun mainNoExitWithRenderer(args: Array<String>, messageRenderer: MessageRenderer) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args, messageRenderer) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }
    }
}

typealias BinaryOptionWithValue<T> = org.jetbrains.kotlin.backend.konan.BinaryOptionWithValue<T>

@Suppress("unused")
fun parseBinaryOptions(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): List<BinaryOptionWithValue<*>> = org.jetbrains.kotlin.backend.konan.parseBinaryOptions(arguments, configuration)

fun main(args: Array<String>) = K2Native.main(args)
fun mainNoExitWithGradleRenderer(args: Array<String>) = K2Native.mainNoExitWithRenderer(args, MessageRenderer.GRADLE_STYLE)
fun mainNoExitWithXcodeRenderer(args: Array<String>) = K2Native.mainNoExitWithRenderer(args, MessageRenderer.XCODE_STYLE)
