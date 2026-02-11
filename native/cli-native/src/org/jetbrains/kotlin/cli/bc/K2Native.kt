/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.native.pipeline.NativeKlibCliPipeline
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.ir.validation.IrValidationException
import org.jetbrains.kotlin.konan.KonanPendingCompilationError
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.config.overrideKonanProperties
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.KotlinPaths

class K2Native : CLICompiler<K2NativeCompilerArguments>() {
    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeCompilerArguments) {}

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    /**
     * Phased pipeline execution for klib compilation.
     * Returns null if this compilation requires binary compilation.
     */
    override fun doExecutePhased(
        arguments: K2NativeCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode? {
        if (arguments.produce != "library") {
            return null
        }
        return doExecutePhasedKlibCompilation(arguments, services, basicMessageCollector, isOneStageCompilation = false)
    }

    private fun doExecutePhasedKlibCompilation(
        arguments: K2NativeCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
        isOneStageCompilation: Boolean
    ): ExitCode {
        // TODO (KT-84069)
        arguments.disableDefaultScriptingPlugin = true
        return NativeKlibCliPipeline(defaultPerformanceManager, isNativeOneStage = isOneStageCompilation).execute(arguments, services, basicMessageCollector)
    }

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${KotlinCompilerVersion.getVersion() ?: "SNAPSHOT"}")
            return ExitCode.OK
        }

        val pluginLoadResult = PluginCliParser.loadPluginsSafe(
            arguments.pluginClasspaths,
            arguments.pluginOptions,
            arguments.pluginConfigurations,
            arguments.pluginOrderConstraints,
            configuration,
            rootDisposable,
        )
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val enoughArguments = arguments.freeArgs.isNotEmpty() || arguments.isUsefulWithoutFreeArgs
        if (!enoughArguments) {
            configuration.messageCollector.report(ERROR, "You have not specified any compilation arguments. No output has been produced.")
        }
        val environment = prepareEnvironment(arguments, configuration, rootDisposable)
        if (CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(configuration)) {
            // Some errors during KotlinCoreEnvironment setup.
            return ExitCode.COMPILATION_ERROR
        }
        try {
            // K2/Native backend cannot produce binary directly from FIR frontend output, since descriptors, deserialized from KLib, are needed
            // So, such compilation is split to two stages:
            // - source files are compiled to intermediate KLib by FIR frontend
            // - intermediate Klib is compiled to binary by K2/Native backend
            if (isOneStageCompilation(arguments)) {
                val intermediateKlib = createIntermediateKlib()
                val klibArgs = prepareKlibArgumentsForOneStage(arguments, intermediateKlib.canonicalPath)
                val klibCompilationExitCode = doExecutePhasedKlibCompilation(
                    klibArgs, Services.EMPTY, configuration.messageCollector,
                    isOneStageCompilation = true
                )
                if (klibCompilationExitCode != ExitCode.OK) {
                    return klibCompilationExitCode
                }
                adjustConfigurationForSecondStage(configuration, intermediateKlib)
                val environmentForSecondStage = prepareEnvironment(arguments, configuration, rootDisposable)
                runKonanDriver(configuration, environmentForSecondStage, rootDisposable)
            } else {
                doExecutePhased(arguments, Services.EMPTY, configuration.messageCollector)
                    ?: runKonanDriver(configuration, environment, rootDisposable)
            }
        } catch (e: Throwable) {
            if (e is KonanCompilationException || e is CompilationErrorException || e is IrValidationException)
                return ExitCode.COMPILATION_ERROR

            if (e is KonanPendingCompilationError) {
                configuration.report(ERROR, e.message)
                return ExitCode.COMPILATION_ERROR
            }

            configuration.report(ERROR, """
                |Compilation failed: ${e.message}

                | * Source files: ${environment.getSourceFiles().joinToString(transform = KtFile::getName)}
                | * Compiler version: ${KotlinCompilerVersion.getVersion()}
                | * Output kind: ${configuration.konanProducedArtifactKind}

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

        configuration.phaseConfig = createPhaseConfig(arguments)

        // Values for keys for non-nullable arguments below must be also copied during 1st stage preparation within `KonanDriver.splitOntoTwoStages()`
        configuration.setupCommonKlibArguments(arguments, canBeMetadataKlibCompilation = true, rootDisposable)

        return environment
    }

    private fun runKonanDriver(
        configuration: CompilerConfiguration,
        environment: KotlinCoreEnvironment,
        rootDisposable: Disposable,
    ) {
        val perfManager = configuration.perfManager

        val konanDriver =
            KonanDriver(environment.project, environment, configuration, perfManager, object : CompilationSpawner {
                override fun spawn(configuration: CompilerConfiguration) {
                    val spawnedArguments = K2NativeCompilerArguments()
                    parseCommandLineArguments(emptyList(), spawnedArguments)
                    val spawnedPerfManager = PerformanceManagerImpl.createChildIfNeeded(perfManager, start = true)
                    configuration.perfManager = spawnedPerfManager
                    val spawnedEnvironment = KotlinCoreEnvironment.createForProduction(
                        rootDisposable, configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES
                    )
                    try {
                        runKonanDriver(configuration, spawnedEnvironment, rootDisposable)
                    } finally {
                        perfManager?.addOtherUnitStats(spawnedPerfManager?.unitStats)
                    }
                }

                override fun spawn(arguments: List<String>, setupConfiguration: CompilerConfiguration.() -> Unit) {
                    val spawnedArguments = K2NativeCompilerArguments()
                    parseCommandLineArguments(arguments, spawnedArguments)
                    val spawnedConfiguration = CompilerConfiguration.create()

                    val spawnedPerfManager = PerformanceManagerImpl.createChildIfNeeded(perfManager, start = true)
                    spawnedConfiguration.messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
                    spawnedConfiguration.perfManager = spawnedPerfManager
                    spawnedConfiguration.setupCommonArguments(spawnedArguments, this@K2Native::createMetadataVersion)
                    spawnedConfiguration.setupFromArguments(spawnedArguments)
                    spawnedConfiguration.setupPartialLinkageConfig(configuration.partialLinkageConfig)
                    configuration.get(CommonConfigurationKeys.USE_FIR)?.let {
                        spawnedConfiguration.put(CommonConfigurationKeys.USE_FIR, it)
                    }
                    configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)?.let {
                        spawnedConfiguration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, it)
                    }
                    configuration[NativeConfigurationKeys.OVERRIDE_KONAN_PROPERTIES]?.let {
                        spawnedConfiguration.overrideKonanProperties = it
                    }
                    configuration.get(BinaryOptions.checkStateAtExternalCalls)?.let {
                        spawnedConfiguration.put(BinaryOptions.checkStateAtExternalCalls, it)
                    }
                    spawnedConfiguration.setupConfiguration()
                    val spawnedEnvironment = prepareEnvironment(spawnedArguments, spawnedConfiguration, rootDisposable)
                    // KT-71976: Should empty `arguments` be provided, prepareEnvironment() resets the keys for 1st compilation stage
                    // In order to keep them, they should be re-initialized with the second invocation of `setupConfiguration()` lambda below.
                    // Meanwhile, the first invocation is still needed to initialize other important keys before `prepareEnvironment()`
                    // TODO KT-72014: Remove the second invocation of `setupConfiguration()`
                    spawnedConfiguration.setupConfiguration()

                    try {
                        runKonanDriver(spawnedConfiguration, spawnedEnvironment, rootDisposable)
                    } finally {
                        perfManager?.addOtherUnitStats(spawnedPerfManager?.unitStats)
                    }
                }
            })

        konanDriver.run()
    }

    private val K2NativeCompilerArguments.isUsefulWithoutFreeArgs: Boolean
        get() {
            if (listTargets || listPhases || checkDependencies) {
                return true
            }
            // A little hack: produce == null is assumed to be treated as produce == "program" later in the pipeline.
            val producingExecutable = produce == null || produce == "program"
            // KT-68673: It is legal to store entry point in one of the libraries.
            if (producingExecutable && libraries?.isNotEmpty() == true) {
                return true
            }
            return !includes.isNullOrEmpty()
                    || !exportedLibraries.isNullOrEmpty()
                    || libraryToAddToCache != null
                    || !compileFromBitcode.isNullOrEmpty()
        }

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

typealias BinaryOptionWithValue<T> = org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptionWithValue<T>

@Suppress("unused")
fun parseBinaryOptions(
    arguments: K2NativeCompilerArguments,
    configuration: CompilerConfiguration,
): List<BinaryOptionWithValue<*>> = org.jetbrains.kotlin.backend.konan.parseBinaryOptions(arguments, configuration)

fun main(args: Array<String>) = K2Native.main(args)
fun mainNoExitWithGradleRenderer(args: Array<String>) = K2Native.mainNoExitWithRenderer(args, MessageRenderer.GRADLE_STYLE)
fun mainNoExitWithXcodeRenderer(args: Array<String>) = K2Native.mainNoExitWithRenderer(args, MessageRenderer.XCODE_STYLE)
