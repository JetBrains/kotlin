/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.blackbox

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.js.test.klib.collectDependencies
import org.jetbrains.kotlin.js.test.klib.customWasmJsCompilerSettings
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerSecondStageFacade
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.test.handlers.WASM_BASE_FILE_NAME
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerSecondStageFacade] for WasmJs.
 */
class CustomWasmJsCompilerSecondStageFacade(
    testServices: TestServices
) : CustomKlibCompilerSecondStageFacade<BinaryArtifacts.Wasm>(testServices) {

    override val outputKind get() = ArtifactKinds.Wasm

    override fun isMainModule(module: TestModule) = module == WasmEnvironmentConfigurator.getMainModule(testServices)
    override fun collectDependencies(module: TestModule) = module.collectDependencies(testServices, CompilationStage.SECOND)

    override fun compileBinary(
        module: TestModule,
        customArgs: List<String>,
        mainLibrary: String,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): BinaryArtifacts.Wasm.Folder {
        val wasmArtifactFile = testServices.temporaryDirectoryManager.getOrCreateTempDirectory(module.name).resolve("$WASM_BASE_FILE_NAME.wasm")
        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            customWasmJsCompilerSettings.customKlibCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::irProduceJs.cliArgument,
                    K2WasmCompilerArguments::wasm.cliArgument,
                    K2JSCompilerArguments::includes.cliArgument(mainLibrary),
                    K2JSCompilerArguments::outputDir.cliArgument, wasmArtifactFile.parentFile.path,
                    K2JSCompilerArguments::moduleName.cliArgument, WASM_BASE_FILE_NAME,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                ),
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        K2JSCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
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
            require(wasmArtifactFile.exists()) {
                "Internal testinfra error: Couldn't find expected generated wasm artifact ${wasmArtifactFile.absolutePath}"
            }

            return BinaryArtifacts.Wasm.Folder(wasmArtifactFile.parentFile)
        } else {
            // Throw an exception to abort further test execution.
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}
