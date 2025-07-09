/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstPhaseFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerFirstPhaseFacade] for JS and Wasm.
 */
class CustomWebCompilerFirstPhaseFacade(testServices: TestServices) : CustomKlibCompilerFirstPhaseFacade(testServices) {
    override val TestModule.customKlibCompilerDefaultLanguageVersion: LanguageVersion
        get() = customWebCompilerSettings(testServices).defaultLanguageVersion

    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices)

    override fun compileKlib(
        module: TestModule,
        customArgs: List<String>,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib {
        val outputKlibFile = File(outputKlibPath).absoluteFile

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            module.customWebCompilerSettings(testServices).customCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::wasm.cliArgument.takeIf { module.isWasmModule(testServices) },
                    K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                    K2JSCompilerArguments::outputDir.cliArgument, outputKlibFile.parentFile.path,
                    K2JSCompilerArguments::moduleName.cliArgument, outputKlibFile.nameWithoutExtension,

                    // Some of the test data files have declarations in the `kotlin` package.
                    // To avoid just suppressing such tests, we allow compiling with the `kotlin` package.
                    CommonCompilerArguments::allowKotlinPackage.cliArgument,
                ),
                runIf(regularDependencies.isNotEmpty()) {
                    listOf(
                        K2JSCompilerArguments::libraries.cliArgument,
                        regularDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(K2JSCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
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
