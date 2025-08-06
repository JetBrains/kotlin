/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.js.test.klib.collectDependencies
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondPhaseFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondPhaseFacade] for Wasm-JS.
 */
class CustomWasmJsCompilerSecondPhaseFacade(
    testServices: TestServices
) : CustomKlibCompilerSecondPhaseFacade<BinaryArtifacts.Wasm>(testServices) {

    override val outputKind get() = ArtifactKinds.Wasm

    override fun isMainModule(module: TestModule) = module == WasmEnvironmentConfigurator.getMainModule(testServices)
    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices)

    override fun compileBinary(
        module: TestModule,
        customArgs: List<String>,
        mainLibrary: String,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): BinaryArtifacts.Wasm {
        require(testServices.compilerConfigurationProvider.getCompilerConfiguration(module).wasmTarget == WasmTarget.JS)

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            customWasmJsCompilerSettings.customCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::wasm.cliArgument,
                    K2JSCompilerArguments::wasmTarget.cliArgument("wasm-js"),

                    K2JSCompilerArguments::irProduceJs.cliArgument,
                    K2JSCompilerArguments::includes.cliArgument(mainLibrary),

                    // TODO: pass artifact directory and artifact name
                    //K2JSCompilerArguments::outputDir.cliArgument, wasmJsArtifactFile.parentFile.path,
                    //K2JSCompilerArguments::moduleName.cliArgument, wasmJsArtifactFile.nameWithoutExtension,
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
            )
        }

        if (exitCode == ExitCode.OK) {
            // Successfully compiled. Return the artifact.
            // TODO: create a valid artifact object
            //return BinaryArtifacts.Wasm(
            //    compilerResult = ???,
            //    compilerResultWithDCE = ???,
            //    compilerResultWithOptimizer = null,
            //)
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }

        TODO("not yet implemented")
    }
}
