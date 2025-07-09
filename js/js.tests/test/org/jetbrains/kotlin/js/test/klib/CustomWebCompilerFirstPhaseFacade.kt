/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.SimpleDiagnosticsCollector
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.frontend.fir.getTransitivesAndFriends
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerFirstPhaseFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.targetPlatformProvider
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * An implementation of [CustomKlibCompilerFirstPhaseFacade] for JS and Wasm.
 */
class CustomWebCompilerFirstPhaseFacade(testServices: TestServices) : CustomKlibCompilerFirstPhaseFacade(testServices) {
    private val TestModule.isWasmModule: Boolean
        get() = testServices.targetPlatformProvider.getTargetPlatform(module = this).isWasm()

    /** Returns null only if this is a JS target. */
    private val TestModule.wasmTargetOrNull: WasmTarget?
        get() = runIf(isWasmModule) {
            testServices.compilerConfigurationProvider.getCompilerConfiguration(module = this).wasmTarget
        }

    override fun collectDependencies(module: TestModule): Pair<Set<String>, Set<String>> {
        val runtimeLibraries: List<File> = when (module.wasmTargetOrNull) {
            null -> { // JS
                listOfNotNull(
                    customJsCompilerSettings.stdlib,
                    // Load "kotlin-test" only when ont of the corresponding directives has been specified.
                    runIf(JsEnvironmentConfigurator.isFullJsRuntimeNeeded(module)) { customJsCompilerSettings.kotlinTest },
                )
            }
            WasmTarget.JS -> {
                listOf(
                    customWasmJsCompilerSettings.stdlib,
                    customWasmJsCompilerSettings.kotlinTest,
                )
            }
            WasmTarget.WASI -> error("WASI target is not yet supported in the first phase of ${CustomWebCompilerFirstPhaseFacade::class.simpleName}")
        }

        val (transitiveLibraries: List<File>, friendLibraries: List<File>) = getTransitivesAndFriends(module, testServices)

        val regularDependencies: Set<String> = buildSet {
            runtimeLibraries.mapTo(this) { it.absolutePath }
            transitiveLibraries.mapTo(this) { it.absolutePath }
        }

        val friendDependencies: Set<String> = friendLibraries.mapToSetOrEmpty { it.absolutePath }

        return regularDependencies to friendDependencies
    }

    override fun compileKlib(
        module: TestModule,
        sources: List<String>,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
        outputKlibPath: String,
    ): BinaryArtifacts.KLib {
        val outputKlibFile = File(outputKlibPath).absoluteFile

        val compilerXmlOutput = ByteArrayOutputStream()

        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            customJsCompilerSettings.customCompiler.callCompiler(
                output = printStream,
                listOfNotNull(
                    K2JSCompilerArguments::wasm.cliArgument.takeIf { module.isWasmModule },
                    K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                    K2JSCompilerArguments::outputDir.cliArgument, outputKlibFile.parentFile.path,
                    K2JSCompilerArguments::moduleName.cliArgument, outputKlibFile.nameWithoutExtension,
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
                sources
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
