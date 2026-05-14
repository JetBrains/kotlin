/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.backend.common.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.backend.konan.CompilationSpawner
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.KonanDriver
import org.jetbrains.kotlin.backend.konan.parseBinaryOptions
import org.jetbrains.kotlin.backend.konan.setupFromArguments
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.isNativeSecondStage
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors.CheckDiagnosticCollector
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.ir.validation.IrValidationException
import org.jetbrains.kotlin.konan.KonanPendingCompilationError
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.config.overrideKonanProperties
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.native.pipeline.NativeKlibCliPipeline
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.util.CompilerType
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.forEachStringMeasurement
import org.jetbrains.kotlin.util.profile

class K2Native : CLICompiler<K2NativeCompilerArguments>() {
    override val platform: TargetPlatform = NativePlatforms.unspecifiedNativePlatform

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    /**
     * Phased pipeline execution for klib compilation.
     * Returns null if this compilation requires binary compilation.
     */
    override fun doExecutePhased(
        arguments: K2NativeCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        if (arguments.isNativeSecondStage()) {
            return doExecuteInAnOldWay(basicMessageCollector, services, arguments)
        }
        return doExecutePhasedKlibCompilation(arguments, services, basicMessageCollector, isOneStageCompilation = false)
    }

    private fun doExecutePhasedKlibCompilation(
        arguments: K2NativeCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
        isOneStageCompilation: Boolean,
    ): ExitCode {
        // TODO (KT-84069)
        arguments.disableDefaultScriptingPlugin = true
        return NativeKlibCliPipeline(defaultPerformanceManager, isNativeOneStage = isOneStageCompilation).execute(
            arguments,
            services,
            basicMessageCollector
        )
    }

    private fun doExecuteInAnOldWay(
        messageCollector: MessageCollector,
        services: Services,
        arguments: K2NativeCompilerArguments,
    ): ExitCode {
        val performanceManager = createPerformanceManager(arguments, services).apply { compilerType = CompilerType.K2 }
        if (arguments.reportPerf || arguments.dumpPerf != null) {
            performanceManager.enableExtendedStats()
        }

        val configuration = CompilerConfiguration.create()

        configuration.put(CLIConfigurationKeys.ORIGINAL_MESSAGE_COLLECTOR_KEY, messageCollector)
        configuration.treatWarningsAsErrors = arguments.allWarningsAsErrors

        val collector = GroupingMessageCollector(messageCollector, arguments.allWarningsAsErrors, arguments.reportAllWarnings).also {
            @OptIn(MessageCollectorAccess::class) // write access
            configuration.messageCollector = it
        }

        configuration.perfManager = performanceManager
        try {
            setupCommonArguments(configuration, arguments)
            configuration.setupFromArguments(arguments)
            if (CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(configuration)) {
                return ExitCode.COMPILATION_ERROR
            }

            val canceledStatus = services[CompilationCanceledStatus::class.java]
            ProgressIndicatorAndCompilationCanceledStatus.setCompilationCanceledStatus(canceledStatus)

            val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
            try {
                setIdeaIoUseFallback()

                val code = doExecute(arguments, configuration, rootDisposable)

                performanceManager.notifyCompilationFinished()
                if (arguments.reportPerf) {
                    collector.report(LOGGING, "PERF: " + performanceManager.getTargetInfo())
                    performanceManager.forEachStringMeasurement {
                        collector.report(LOGGING, "PERF: $it", null)
                    }
                }

                if (arguments.dumpPerf != null) {
                    performanceManager.dumpPerformanceReport(arguments.dumpPerf!!)
                }

                return if (CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(configuration)) ExitCode.COMPILATION_ERROR else code
            } catch (e: CompilationCanceledException) {
                collector.reportCompilationCancelled(e)
                return ExitCode.OK
            } catch (e: RuntimeException) {
                val cause = e.cause
                if (cause is CompilationCanceledException) {
                    collector.reportCompilationCancelled(cause)
                    return ExitCode.OK
                } else {
                    throw e
                }
            } finally {
                disposeRootInWriteAction(rootDisposable)
            }
        } catch (_: CompilationErrorException) {
            return ExitCode.COMPILATION_ERROR
        } catch (t: Throwable) {
            MessageCollectorUtil.reportException(collector, t)
            return if (t is OutOfMemoryError || t.hasOOMCause()) ExitCode.OOM_ERROR else ExitCode.INTERNAL_ERROR
        } finally {
            collector.flush()
        }
    }

    private fun setupCommonArguments(configuration: CompilerConfiguration, arguments: K2NativeCompilerArguments) {
        configuration.setupCommonArguments(arguments, this::createMetadataVersion)
    }

    private fun doExecute(
        @NotNull arguments: K2NativeCompilerArguments,
        @NotNull configuration: CompilerConfiguration,
        @NotNull rootDisposable: Disposable,
    ): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${KotlinCompilerVersion.getVersion() ?: "SNAPSHOT"}")
            return ExitCode.OK
        }

        try {
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
                configuration.report(CliDiagnostics.KONAN_ARGUMENT_ERROR, "You have not specified any compilation arguments. No output has been produced.")
            }
            val environment = prepareEnvironment(arguments, configuration, rootDisposable)
            if (CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(configuration)) {
                // Some errors during KotlinCoreEnvironment setup.
                return ExitCode.COMPILATION_ERROR
            }
            // K2/Native backend cannot produce binary directly from FIR frontend output, since descriptors, deserialized from KLib, are needed
            // So, such compilation is split to two stages:
            // - source files are compiled to intermediate KLib by FIR frontend
            // - intermediate Klib is compiled to binary by K2/Native backend
            if (isOneStageCompilation(arguments)) {
                val intermediateKlib = createIntermediateKlib()
                val klibArgs = prepareKlibArgumentsForOneStage(arguments, intermediateKlib.canonicalPath)
                val klibCompilationExitCode = doExecutePhasedKlibCompilation(
                    klibArgs, Services.EMPTY, @OptIn(MessageCollectorAccess::class) configuration.messageCollector,
                    isOneStageCompilation = true
                )
                if (klibCompilationExitCode != ExitCode.OK) {
                    return klibCompilationExitCode
                }
                adjustConfigurationForSecondStage(configuration, intermediateKlib)
                val environmentForSecondStage = prepareEnvironment(arguments, configuration, rootDisposable)
                runKonanDriver(configuration, environmentForSecondStage, rootDisposable)
            } else {
                if (arguments.isNativeSecondStage()) {
                    runKonanDriver(configuration, environment, rootDisposable)
                } else {
                    doExecutePhasedKlibCompilation(
                        arguments,
                        Services.EMPTY,
                        @OptIn(MessageCollectorAccess::class) configuration.messageCollector,
                        isOneStageCompilation = false
                    )
                }
            }
        } catch (e: Throwable) {
            if (e is KonanCompilationException || e is CompilationErrorException || e is IrValidationException)
                return ExitCode.COMPILATION_ERROR

            if (e is KonanPendingCompilationError) {
                configuration.report(CliDiagnostics.KONAN_COMPILATION_ERROR, e.message)
                return ExitCode.COMPILATION_ERROR
            }

            configuration.report(
                CliDiagnostics.KONAN_COMPILATION_ERROR, """
                |Compilation failed: ${e.message}

                | * Compiler version: ${KotlinCompilerVersion.getVersion()}
                | * Output kind: ${configuration.konanProducedArtifactKind}

                """.trimMargin()
            )
            throw e
        } finally {
            CheckDiagnosticCollector.reportToMessageCollector(configuration)
        }

        return ExitCode.OK
    }

    private fun prepareEnvironment(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
    ): KotlinCoreEnvironment {
        val environment = KotlinCoreEnvironment.createForProduction(
            rootDisposable,
            configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES
        )

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
                    @OptIn(MessageCollectorAccess::class) // write access
                    spawnedConfiguration.messageCollector = configuration.messageCollector
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

                    if (CheckDiagnosticCollector.checkHasErrorsAndReportToMessageCollector(spawnedConfiguration)) {
                        // Some errors during KotlinCoreEnvironment setup.
                        throw CompilationErrorException()
                    }

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
            if (producingExecutable && libraries.isNotEmpty()) {
                return true
            }
            return includes.isNotEmpty()
                    || exportedLibraries.isNotEmpty()
                    || libraryToAddToCache != null
                    || !compileFromBitcode.isNullOrEmpty()
        }

    override fun createArguments(): K2NativeCompilerArguments = K2NativeCompilerArguments()

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            profile("Total compiler main()") {
                doMain(K2Native(), args)
            }
        }

        @JvmStatic
        fun mainNoExit(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }

        @JvmStatic
        fun mainNoExitWithRenderer(args: Array<String>, messageRenderer: MessageRenderer) {
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
): List<BinaryOptionWithValue<*>> = parseBinaryOptions(arguments, configuration)

fun main(args: Array<String>) = K2Native.main(args)
fun mainNoExitWithGradleRenderer(args: Array<String>) = K2Native.mainNoExitWithRenderer(args, MessageRenderer.GRADLE_STYLE)
fun mainNoExitWithXcodeRenderer(args: Array<String>) = K2Native.mainNoExitWithRenderer(args, MessageRenderer.XCODE_STYLE)
