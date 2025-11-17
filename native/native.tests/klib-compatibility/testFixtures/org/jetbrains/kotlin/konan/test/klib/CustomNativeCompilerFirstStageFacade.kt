/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstStageFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerFirstStageFacade] for Native.
 */
class CustomNativeCompilerFirstStageFacade(testServices: TestServices) : CustomKlibCompilerFirstStageFacade(testServices) {
    override val TestModule.customKlibCompilerDefaultLanguageVersion: LanguageVersion
        get() = customNativeCompilerSettings.defaultLanguageVersion

    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices)

    override fun compileKlib(
        module: TestModule,
        customArgs: List<String>,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib {
        val regularAndFriendDependencies = regularDependencies + friendDependencies
        val outputKlibFile = File(outputKlibPath).absoluteFile

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            customNativeCompilerSettings.compiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    K2NativeCompilerArguments::produce.cliArgument, "library",
                    K2NativeCompilerArguments::moduleName.cliArgument, module.name,
                    K2NativeCompilerArguments::outputName.cliArgument, outputKlibFile.path,
                    K2NativeCompilerArguments::kotlinHome.cliArgument, customNativeCompilerSettings.nativeHome.absolutePath,
                    K2NativeCompilerArguments::target.cliArgument, "macos_arm64",
                    K2NativeCompilerArguments::enableAssertions.cliArgument,
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
                sources,
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            return BinaryArtifacts.KLib(outputKlibFile, SimpleDiagnosticsCollector(BaseDiagnosticsCollector.RawReporter.DO_NOTHING))
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}
