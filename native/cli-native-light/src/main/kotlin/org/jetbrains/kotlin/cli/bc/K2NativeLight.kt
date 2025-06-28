package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jebrains.kotlin.backend.native.BaseNativeConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jebrains.kotlin.backend.native.report
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.NativeFrontendDriver
import org.jetbrains.kotlin.backend.common.IrValidationError
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.createPhaseConfig
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.perfManager
import org.jetbrains.kotlin.cli.common.setupCommonKlibArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.konan.KonanPendingCompilationError
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.psi.KtFile

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class K2NativeLight : CLICompiler<K2NativeCompilerArguments>() {
    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeCompilerArguments) {}

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${KotlinCompilerVersion.getVersion() ?: "SNAPSHOT"}")
            return ExitCode.OK
        }

        val pluginLoadResult = PluginCliParser.loadPluginsSafe(
            arguments.pluginClasspaths, arguments.pluginOptions, arguments.pluginConfigurations, configuration, rootDisposable,
        )
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val enoughArguments = arguments.freeArgs.isNotEmpty() || arguments.isUsefulWithoutFreeArgs
        if (!enoughArguments) {
            configuration.messageCollector.report(ERROR, "You have not specified any compilation arguments. No output has been produced.")
        }
        val environment = prepareEnvironment(arguments, configuration, rootDisposable)
        if (configuration.messageCollector.hasErrors()) {
            // Some errors during KotlinCoreEnvironment setup.
            return ExitCode.COMPILATION_ERROR
        }

        try {
            runKonanDriver(configuration, environment, rootDisposable)
        } catch (e: Throwable) {
            if (e is KonanCompilationException || e is CompilationErrorException || e is IrValidationError)
                return ExitCode.COMPILATION_ERROR

            if (e is KonanPendingCompilationError) {
                configuration.report(ERROR, e.message)
                return ExitCode.COMPILATION_ERROR
            }

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

        configuration.phaseConfig = createPhaseConfig(arguments)

        // Values for keys for non-nullable arguments below must be also copied during 1st stage preparation within `KonanDriver.splitOntoTwoStages()`
        configuration.setupCommonKlibArguments(arguments, canBeMetadataKlibCompilation = true)
        arguments.dumpSyntheticAccessorsTo?.let { configuration.put(KlibConfigurationKeys.SYNTHETIC_ACCESSORS_DUMP_DIR, it) }

        return environment
    }

    private fun runKonanDriver(
        configuration: CompilerConfiguration,
        environment: KotlinCoreEnvironment,
        rootDisposable: Disposable,
        spawning: Boolean = false,
    ) {
        val mainPerfManager = configuration.perfManager
        val childPerfManager = if (spawning) {
            if (mainPerfManager?.isPhaseMeasuring == true) {
                mainPerfManager.notifyPhaseFinished(PhaseType.Initialization)
            }
            PerformanceManagerImpl.createAndEnableChildIfNeeded(mainPerfManager)
        } else {
            null
        }

        try {
            val baseNativeConfig = BaseNativeConfig(configuration)
            NativeFrontendDriver(childPerfManager).run(baseNativeConfig, environment, environment.project)
        } finally {
            mainPerfManager?.addOtherUnitStats(childPerfManager?.unitStats)
        }
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
                doMain(K2NativeLight(), args)
            }
        }

        @JvmStatic fun mainNoExit(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2NativeLight(), args) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }

        @JvmStatic fun mainNoExitWithRenderer(args: Array<String>, messageRenderer: MessageRenderer) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2NativeLight(), args, messageRenderer) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }
    }
}

fun main(args: Array<String>) = K2NativeLight.main(args)
fun mainNoExitWithGradleRenderer(args: Array<String>) = K2NativeLight.mainNoExitWithRenderer(args, MessageRenderer.GRADLE_STYLE)
fun mainNoExitWithXcodeRenderer(args: Array<String>) = K2NativeLight.mainNoExitWithRenderer(args, MessageRenderer.XCODE_STYLE)
