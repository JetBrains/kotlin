/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.konan.test.blackbox.support.AssertionsMode
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FILECHECK_STAGE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.withPlatformLibs
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.GroupingPhaseInputArtifact
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives.WITH_PLATFORM_LIBS
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for Native.
 * Suits any version backend version: either current, or old released. It's specified by `nativeCompilerSettings` param.
 */
class NativeCompilerSecondStageFacade private constructor(
    val testServices: TestServices,
    private val customNativeCompilerSettings: CustomNativeCompilerSettings
) {
    class NonGrouping(
        testServices: TestServices,
        private val customNativeCompilerSettings: CustomNativeCompilerSettings,
    ) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Native>(testServices) {
        override val outputKind get() = ArtifactKinds.Native
        override fun isMainModule(module: TestModule): Boolean {
            return NativeEnvironmentConfigurator.isMainModule(module, testServices.moduleStructure)
        }

        override fun collectDependencies(module: TestModule): Pair<Set<String>, Set<String>> = module.collectDependencies(testServices)

        override fun compileBinary(
            module: TestModule,
            customArgs: List<String>,
            mainLibrary: String,
            regularDependencies: Set<String>,
            friendDependencies: Set<String>,
        ): BinaryArtifacts.Native {
            val facade = NativeCompilerSecondStageFacade(testServices, customNativeCompilerSettings)
            val (exitCode, output, executableFile) = facade.runCli(
                dirName = File(mainLibrary).name,
                executableFileName = module.name + ".kexe",
                fileCheckStage = module.fileCheckStage(),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
                mainLibraries = listOf(mainLibrary),
                enableAssertions = AssertionsMode.ALWAYS_DISABLE !in module.directives[ASSERTIONS_MODE],
                withPlatformLibs = module.directives.contains(WITH_PLATFORM_LIBS),
                customLanguageFeatures = module.directives[LanguageSettingsDirectives.LANGUAGE],
                freeArgs = module.directives[FREE_COMPILER_ARGS] + customArgs,
            )

            if (exitCode == ExitCode.OK) {
                // Successfully compiled. Return the artifact.
                return BinaryArtifacts.Native(executableFile)
            } else {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }
    }

    class Grouping(
        val testServices: TestServices,
        private val customNativeCompilerSettings: CustomNativeCompilerSettings
    ) : AbstractGroupingPhaseTestFacade<GroupingPhaseInputArtifact, BinaryArtifacts.Native>() {
        override fun transform(inputArtifact: GroupingPhaseInputArtifact): BinaryArtifacts.Native {
            val someModule = inputArtifact.nonGroupingPhaseOutputs.first().testServices.moduleStructure.modules.last()
            var someLibrary: File? = null

            val regularDependencies = mutableSetOf<String>()
            val friendDependencies = mutableSetOf<String>()
            val mainLibraries = mutableListOf<String>()
            for ((services, _) in inputArtifact.nonGroupingPhaseOutputs) {
                val mainModule = services.moduleStructure.modules.last()
                mainModule.collectDependencies(services).let { (regular, friend) ->
                    regularDependencies += regular
                    friendDependencies += friend
                }
                val mainLibrary = services.artifactsProvider.getArtifact(mainModule, ArtifactKinds.KLib).outputFile
                mainLibraries += mainLibrary.absolutePath
                if (someLibrary == null) someLibrary = mainLibrary
            }

            val facade = NativeCompilerSecondStageFacade(testServices, customNativeCompilerSettings)
            val (exitCode, output, executableFile) = facade.runCli(
                dirName = someLibrary!!.name,
                executableFileName = someModule.name + ".kexe",
                fileCheckStage = someModule.fileCheckStage(),
                regularDependencies = regularDependencies,
                friendDependencies = friendDependencies,
                mainLibraries = mainLibraries,
                enableAssertions = AssertionsMode.ALWAYS_DISABLE !in someModule.directives[ASSERTIONS_MODE],
                withPlatformLibs = someModule.directives.contains(WITH_PLATFORM_LIBS),
                customLanguageFeatures = someModule.directives[LanguageSettingsDirectives.LANGUAGE],
                freeArgs = someModule.directives[FREE_COMPILER_ARGS] + "-Xklib-duplicated-unique-name-strategy=allow-all-with-warning",
            )

            if (exitCode == ExitCode.OK) {
                // Successfully compiled. Return the artifact.
                return BinaryArtifacts.Native(executableFile)
            } else {
                // Throw an exception to abort further test execution.
                throw CustomKlibCompilerException(exitCode, output.toString(Charsets.UTF_8.name()))
            }
        }

        override val inputKind: TestArtifactKind<GroupingPhaseInputArtifact>
            get() = GroupingPhaseInputArtifact.Kind
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

    fun getNativeArtifactsOutputDir(testServices: TestServices, moduleName: String): File {
        return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(moduleName)
    }

    data class CliRunResult(val exitCode: ExitCode, val output: ByteArrayOutputStream, val executableFile: File)

    fun runCli(
        dirName: String,
        executableFileName: String,
        fileCheckStage: String?,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        mainLibraries: List<String>,
        enableAssertions: Boolean,
        withPlatformLibs: Boolean,
        customLanguageFeatures: List<String>,
        freeArgs: List<String>
    ): CliRunResult {
        val executableFile = getNativeArtifactsOutputDir(testServices, dirName).resolve(executableFileName)

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customNativeCompilerSettings.compiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2NativeCompilerArguments::kotlinHome.cliArgument, nativeHome.absolutePath,
                    optimizationArgument.cliArgument,
                    K2NativeCompilerArguments::binaryOptions.cliArgument("runtimeAssertionsMode=panic"),
                    K2NativeCompilerArguments::verifyIr.cliArgument("error"),
                    K2NativeCompilerArguments::llvmVariant.cliArgument("dev"),
                    K2NativeCompilerArguments::produce.cliArgument, "program",
                    K2NativeCompilerArguments::outputName.cliArgument, executableFile.path,
                    K2NativeCompilerArguments::generateTestRunner.cliArgument,
                    K2NativeCompilerArguments::testDumpOutputPath.cliArgument(executableFile.resolveSibling("${executableFile.name}.dump").path),
                    *mainLibraries.map { mainLibrary ->
                        K2NativeCompilerArguments::includes.cliArgument(mainLibrary)
                    }.toTypedArray(),
                    K2NativeCompilerArguments::autoCacheableFrom.cliArgument(nativeHome.resolve("klib").absolutePath)
                        .takeIf { cacheMode.useStaticCacheForDistributionLibraries },
                    K2NativeCompilerArguments::enableAssertions.cliArgument.takeIf { enableAssertions },
                    K2NativeCompilerArguments::target.cliArgument, kotlinNativeTargets.testTarget.name,
                    K2NativeCompilerArguments::nodefaultlibs.cliArgument.takeIf {
                        !(this.withPlatformLibs || withPlatformLibs)
                    },
                ),
                regularAndFriendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::libraries.cliArgument, it)
                },
                friendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::friendModules.cliArgument, it)
                },
                customLanguageFeatures.map {
                    CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it"
                },
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
        ?: error("Exactly one argument for FILECHECK directive is needed: LLVM stage name to dump bitcode after, in files: $files")
}

/**
 * Constructs file check dump path for the given executable file and stage
 */
internal fun File.fileCheckDump(fileCheckStage: String): File = this.resolveSibling("out.$fileCheckStage.ll")
