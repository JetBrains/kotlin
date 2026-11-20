/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.VerifyIrMode
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.RuntimeAssertsMode
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.AssertionsMode
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FILECHECK_STAGE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.group.collectToggledCheckers
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.withPlatformLibs
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.GroupingStageInputArtifact
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * The second stage of Native tests: links KLIBs into an executable with the Kotlin/Native compiler CLI.
 * Suits any backend version: either current, or old released. It's specified by the `customNativeCompilerSettings` param.
 */
class NativeCompilerSecondStageFacade private constructor(
    val testServices: TestServices,
    private val customNativeCompilerSettings: CustomNativeCompilerSettings
) {
    /**
     * @param isCompatibilityTesting whether the batch is linked in a KLIB compatibility test, where either the KLIBs or the linker
     *   come from a previously released compiler, so the IR of the KLIBs is not required to pass the IR validation of the linker.
     */
    class Grouping(
        val testServices: TestServices,
        private val customNativeCompilerSettings: CustomNativeCompilerSettings,
        private val isCompatibilityTesting: Boolean,
    ) : AbstractGroupingStageTestFacade<GroupingStageInputArtifact, BinaryArtifacts.Native>() {
        override fun transform(inputArtifact: GroupingStageInputArtifact): BinaryArtifacts.Native {
            val servicesOfSomeModule = inputArtifact.nonGroupingStageOutputs.first().testServices
            val someModule = servicesOfSomeModule.moduleStructure.modules.last()
            var someLibrary: File? = null
            val freeArgs = someModule.directives[FREE_COMPILER_ARGS]

            val regularDependencies = mutableSetOf<String>()
            val friendDependencies = mutableSetOf<String>()
            val mainLibraries = mutableListOf<String>()
            val facade = NativeCompilerSecondStageFacade(testServices, customNativeCompilerSettings)
            for ((val services = testServices, val _ = catchingExecutor) in inputArtifact.nonGroupingStageOutputs) {
                checkTargetOfTest(services, facade.kotlinNativeTargets.testTarget)
                val mainModule = services.moduleStructure.modules.last()
                mainModule.collectDependencies(services, CompilationStage.SECOND).let { [regular, friend] ->
                    regularDependencies += regular
                    friendDependencies += friend
                }
                val mainLibrary = services.artifactsProvider.getArtifact(mainModule, ArtifactKinds.KLib).outputFile
                mainLibraries += mainLibrary.absolutePath
                if (someLibrary == null) someLibrary = mainLibrary
                val freeArgsOfTest = mainModule.directives[FREE_COMPILER_ARGS]
                testServices.assertions.assertTrue(freeArgs.toSet() == freeArgsOfTest.toSet()) {
                    buildString {
                        appendLine("Free compiler args are not equal for two tests in batch:")
                        appendLine("Test ${servicesOfSomeModule.testInfo.methodName}: $freeArgs")
                        appendLine("Test ${services.testInfo.methodName}: $freeArgsOfTest")
                    }
                }
            }

            // In grouping mode the module name is being escaped with the test info, which could produce
            // quite a big executable file path. This leads to problems on windows, as there is a hard
            // limit of 260 characters for executable file path. So we use the hash of the module name
            // instead.
            val moduleNameHash = someModule.name.hashCode().toHexString()
            @OptIn(ExperimentalCompilerArgument::class)
            val (exitCode, output, executableFile) = facade.runCli(
                dirName = someLibrary!!.resolveSibling(moduleNameHash).absolutePath,
                executableFileName = "$moduleNameHash.${facade.executableExtension}",
                fileCheckStage = someModule.fileCheckStage(),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
                mainLibraries = mainLibraries,
                enableAssertions = AssertionsMode.ALWAYS_DISABLE !in someModule.directives[ASSERTIONS_MODE],
                withPlatformLibs = someModule.directives.contains(WITH_PLATFORM_LIBS),
                freeArgs = freeArgs + irCheckersArguments(someModule) + "-Xklib-duplicated-unique-name-strategy=allow-all-with-warning",
                verifyIrMode = if (isCompatibilityTesting) VerifyIrMode.NONE else VerifyIrMode.ERROR,
            )

            if (exitCode == ExitCode.OK) {
                // Successfully compiled. Return the artifact.
                return BinaryArtifacts.Native(executableFile)
            } else {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }

        /**
         * Checks that every module of a test is configured for the [linkTarget] of its batch. A test class enforcing the host target
         * leaves the target of its modules unconfigured. The target is not taken from the compiler configuration of the test:
         * building it runs the environment configurators, whose side effects (like setting `kotlin.native.home`) are not meant for
         * the thread of the batch.
         */
        private fun checkTargetOfTest(services: TestServices, linkTarget: KonanTarget) {
            if (services.testInfo.enforcedHostTarget) return
            for (module in services.moduleStructure.modules) {
                val moduleTarget = services.nativeEnvironmentConfigurator.getNativeTarget(module)
                // Test-infrastructure invariant violation (not a failure of the code under test): throw a
                // TestInfrastructureException so it is never masked by failure suppressors (e.g. an IGNORE_BACKEND directive).
                checkTestInfrastructure(moduleTarget == linkTarget) {
                    "Internal error: module ${module.name} of test ${services.testInfo.methodName} is configured for $moduleTarget, " +
                            "but its batch is linked for $linkTarget."
                }
            }
        }

        override val inputKind: TestArtifactKind<GroupingStageInputArtifact>
            get() = GroupingStageInputArtifact.Kind
        override val outputKind: TestArtifactKind<BinaryArtifacts.Native>
            get() = ArtifactKinds.Native
    }

    val testRunSettings = testServices.testRunSettings
    val cacheMode = testRunSettings.get<CacheMode>()
    val optimizationMode = testRunSettings.get<OptimizationMode>()
    val optimizationArgument = if (optimizationMode == OptimizationMode.OPT)
        K2NativeCompilerArguments::optimization
    else
        K2NativeCompilerArguments::debug
    val nativeHome = customNativeCompilerSettings.nativeHome
    val kotlinNativeTargets = testRunSettings.get<KotlinNativeTargets>()
    val withPlatformLibs = testRunSettings.withPlatformLibs

    val executableExtension: String
        get() = when {
            System.getProperty("os.name").lowercase().startsWith("windows") -> "exe"
            else -> "kexe"
        }

    fun getNativeArtifactsOutputDir(testServices: TestServices, moduleName: String): File {
        return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(moduleName)
    }

    data class CliRunResult(val exitCode: ExitCode, val output: ByteArrayOutputStream, val executableFile: File)

    @OptIn(ExperimentalCompilerArgument::class)
    fun runCli(
        dirName: String,
        executableFileName: String,
        fileCheckStage: String?,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        mainLibraries: List<String>,
        enableAssertions: Boolean,
        withPlatformLibs: Boolean,
        freeArgs: List<String>,
        verifyIrMode: VerifyIrMode,
    ): CliRunResult {
        val executableFile = getNativeArtifactsOutputDir(testServices, dirName).resolve(executableFileName)

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            val friendModules = friendDependencies.joinToString(File.pathSeparator)
            customNativeCompilerSettings.compiler.callCompiler(
                output = printStream,
                buildList {
                    addAll(listOf(K2NativeCompilerArguments::kotlinHome.cliArgument, nativeHome.absolutePath))
                    add(optimizationArgument.cliArgument)
                    add(K2NativeCompilerArguments::binaryOptions.cliArgument("${BinaryOptions.runtimeAssertionsMode}=${RuntimeAssertsMode.PANIC}"))
                    add(K2NativeCompilerArguments::verifyIr.cliArgument(verifyIrMode.name))
                    add(K2NativeCompilerArguments::llvmVariant.cliArgument("dev"))
                    addAll(listOf(K2NativeCompilerArguments::produce.cliArgument, "program"))
                    addAll(listOf(K2NativeCompilerArguments::outputName.cliArgument, executableFile.path))
                    add(K2NativeCompilerArguments::generateTestRunner.cliArgument)
                    add(K2NativeCompilerArguments::testDumpOutputPath
                        .cliArgument(executableFile.resolveSibling("${executableFile.name}.dump").path))
                    addAll(mainLibraries.map { mainLibrary ->
                        K2NativeCompilerArguments::includes.cliArgument(mainLibrary)
                    })
                    if (cacheMode.useStaticCacheForDistributionLibraries) {
                        add(K2NativeCompilerArguments::autoCacheableFrom.cliArgument(nativeHome.resolve("klib").absolutePath))
                        add(K2NativeCompilerArguments::binaryOptions.cliArgument("${BinaryOptions.enableReleaseBinaryCache}=true"))
                    }
                    if (enableAssertions) {
                        add(K2NativeCompilerArguments::enableAssertions.cliArgument)
                    }
                    addAll(listOf(K2NativeCompilerArguments::target.cliArgument, kotlinNativeTargets.testTarget.name)) // consider getting it from compilerConfiguration
                    if (!(this@NativeCompilerSecondStageFacade.withPlatformLibs || withPlatformLibs)) {
                        add(K2NativeCompilerArguments::nodefaultlibs.cliArgument)
                    }
                    addAll(customNativeCompilerSettings.provisionedXcodeArgs)
                },
                regularAndFriendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::libraries.cliArgument, it)
                },
                listOf(K2NativeCompilerArguments::friendModules.cliArgument, friendModules).takeIf { friendModules.isNotEmpty() },
                freeArgs,
                fileCheckStage?.let {
                    listOf(
                        K2NativeCompilerArguments::saveLlvmIrAfter.cliArgument(it),
                        K2NativeCompilerArguments::saveLlvmIrDirectory.cliArgument(executableFile.fileCheckDump(fileCheckStage).parent),
                    )
                },
            )
        }
        return CliRunResult(exitCode, compilerXmlOutput, executableFile)
    }
}

internal fun TestModule.fileCheckStage(): String? {
    if (!directives.contains(FILECHECK_STAGE))
        return null
    return directives[FILECHECK_STAGE].singleOrNull()
    // Test-infrastructure invariant violation (not a failure of the code under test): throw a
    // TestInfrastructureException so it is never masked by failure suppressors (e.g. an IGNORE_BACKEND directive).
        ?: testInfraError(
            "Exactly one argument for FILECHECK directive is needed: LLVM stage name to dump bitcode after, in files: $files"
        )
}

/**
 * Constructs file check dump path for the given executable file and stage
 */
internal fun File.fileCheckDump(fileCheckStage: String): File = this.resolveSibling("out.$fileCheckStage.ll")

fun irCheckersArguments(module: TestModule): List<String> =
    module.directives.collectToggledCheckers().let { [additional, disabled] ->
        val additionalArgs = additional.ifNotEmpty { "-Xadditional-ir-checkers=" + additional.joinToString(",") }
        val disabledArgs = disabled.ifNotEmpty { "-Xdisable-ir-checkers=" + disabled.joinToString(",") }
        listOfNotNull(additionalArgs, disabledArgs)
    }
