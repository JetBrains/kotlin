/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for Native.
 */
class NativeCompilerSecondStageFacade(
    testServices: TestServices,
    private val nativeCompilerSettings: NativeCompilerSettings,
) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Native>(testServices) {

    override val outputKind get() = ArtifactKinds.Native

    override fun isMainModule(module: TestModule) = NativeEnvironmentConfigurator.isMainModule(module)
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
            nativeCompilerSettings.compiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2NativeCompilerArguments::produce.cliArgument, "program",
                    K2NativeCompilerArguments::includes.cliArgument(mainLibrary),
                    K2NativeCompilerArguments::outputName.cliArgument, executableFile.path,
                    K2NativeCompilerArguments::kotlinHome.cliArgument, nativeCompilerSettings.nativeHome.absolutePath,
                    K2NativeCompilerArguments::target.cliArgument, "macos_arm64",
                    K2NativeCompilerArguments::enableAssertions.cliArgument,
                    K2NativeCompilerArguments::verifyIr.cliArgument("none"),
                    K2NativeCompilerArguments::binaryOptions.cliArgument("gc=parallel_mark_concurrent_sweep"),
                    K2NativeCompilerArguments::binaryOptions.cliArgument("runtimeAssertionsMode=panic"),
                    K2NativeCompilerArguments::llvmVariant.cliArgument("dev"),
                    K2NativeCompilerArguments::generateTestRunner.cliArgument,
                    K2NativeCompilerArguments::testDumpOutputPath.cliArgument(executableFile.path+".dump"),
                    K2NativeCompilerArguments::partialLinkageMode.cliArgument("enable"),
                    K2NativeCompilerArguments::partialLinkageLogLevel.cliArgument("error"),
                    K2NativeCompilerArguments::autoCacheableFrom.cliArgument(nativeCompilerSettings.nativeHome.resolve("klib").absolutePath),
                    K2NativeCompilerArguments::backendThreads.cliArgument("1"),
                    K2NativeCompilerArguments::nodefaultlibs.cliArgument,
                    K2NativeCompilerArguments::optIn.cliArgument("kotlin.native.internal.InternalForKotlinNative"),
                    K2NativeCompilerArguments::optIn.cliArgument("kotlin.native.internal.InternalForKotlinNativeTests"),
                ),
                regularAndFriendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::libraries.cliArgument, it)
                },
                friendDependencies.flatMap {
                    listOf(K2NativeCompilerArguments::friendModules.cliArgument, it)
                },
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
