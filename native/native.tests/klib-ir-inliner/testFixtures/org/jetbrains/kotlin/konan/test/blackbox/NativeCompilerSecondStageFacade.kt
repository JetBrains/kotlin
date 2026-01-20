/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.AssertionsMode
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.ASSERTIONS_MODE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FREE_COMPILER_ARGS
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
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
class NativeCompilerSecondStageFacade(
    testServices: TestServices,
    private val customNativeCompilerSettings: CustomNativeCompilerSettings,
) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Native>(testServices) {

    val testRunSettings = testServices.testRunSettings
    val cacheMode = testRunSettings.get<CacheMode>()
    val optimizationMode = testRunSettings.get<OptimizationMode>()
    val nativeHome = customNativeCompilerSettings.nativeHome

    override val outputKind get() = ArtifactKinds.Native
    override fun isMainModule(module: TestModule) = NativeEnvironmentConfigurator.isMainModule(module, testServices.moduleStructure)
    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices)

    fun getNativeArtifactsOutputDir(testServices: TestServices, moduleName: String): File {
        return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(moduleName)
    }

    override fun compileBinary(
        module: TestModule,
        customArgs: List<String>,
        mainLibrary: String,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): BinaryArtifacts.Native {
        val executableFile = getNativeArtifactsOutputDir(testServices, File(mainLibrary).name).resolve(module.name + ".kexe")

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customNativeCompilerSettings.compiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2NativeCompilerArguments::kotlinHome.cliArgument, nativeHome.absolutePath,
                    K2NativeCompilerArguments::debug.cliArgument,
                    K2NativeCompilerArguments::binaryOptions.cliArgument("runtimeAssertionsMode=panic"),
                    K2NativeCompilerArguments::binaryOptions.cliArgument("gc=parallel_mark_concurrent_sweep"),
                    K2NativeCompilerArguments::verifyIr.cliArgument("error"),
                    K2NativeCompilerArguments::llvmVariant.cliArgument("dev"),
                    K2NativeCompilerArguments::produce.cliArgument, "program",
                    K2NativeCompilerArguments::outputName.cliArgument, executableFile.path,
                    K2NativeCompilerArguments::generateTestRunner.cliArgument,
                    K2NativeCompilerArguments::includes.cliArgument(mainLibrary),
                    K2NativeCompilerArguments::autoCacheableFrom.cliArgument(nativeHome.resolve("klib").absolutePath)
                        .takeIf { cacheMode.useStaticCacheForDistributionLibraries },
                    K2NativeCompilerArguments::nodefaultlibs.cliArgument,
                    K2NativeCompilerArguments::enableAssertions.cliArgument.takeIf {
                        AssertionsMode.ALWAYS_DISABLE !in module.directives[ASSERTIONS_MODE]
                    },
                    K2NativeCompilerArguments::optimization.cliArgument.takeIf { optimizationMode == OptimizationMode.OPT },
                ),
                regularAndFriendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::libraries.cliArgument, it)
                },
                friendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::friendModules.cliArgument, it)
                },
                module.directives[FREE_COMPILER_ARGS],
                customArgs,
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.

            return BinaryArtifacts.Native(executableFile)
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}
