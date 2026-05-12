/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.converters

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.KotlinWasmCompiler
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * A facade for the **first stage** of the two-stage K/Wasm test compilation pipeline: compiling Kotlin
 * sources of a single test (or the synthetic batch launcher) into a per-test KLIB by invoking the current
 * version of the K/Wasm CLI compiler ([KotlinWasmCompiler]).
 *
 * This logic is shared by the second-stage grouping facades
 * ([org.jetbrains.kotlin.wasm.test.converters.WasmInProcessSecondStageFacade.Grouping] and
 * [org.jetbrains.kotlin.wasm.test.blackbox.CustomWasmSecondStageFacade.Grouping]), which need to compile the
 * synthesized launcher source into a small `launcher.klib` before linking it together with the per-test KLIBs.
 */
class WasmFirstStageFacade(
    val testServices: TestServices,
) {
    fun callCompiler(output: PrintStream, vararg args: List<String>?): ExitCode {
        val allArgs = args.flatMap { it.orEmpty() }.toTypedArray()
        return KotlinWasmCompiler().execFullPathsInMessages(output, allArgs)
    }

    fun compileSourcesToKlib(
        module: TestModule,
        sources: List<File>,
        klibOutputFile: File,
        languageVersion: String,
        customLanguageFeatures: List<String>,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ): ExitCode {
        val returnValueCheckerModes: List<ReturnValueCheckerMode> = module.directives[RETURN_VALUE_CHECKER_MODE]
        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val isWasmWasi = module.targetPlatform(testServices).isWasmWasi()
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            callCompiler(
                output = printStream,
                listOfNotNull(
                    runIf(isWasmWasi) {
                        KotlinWasmCompilerArguments::wasmTarget.cliArgument(WasmTarget.WASI.alias)
                    },
                    CommonCompilerArguments::languageVersion.cliArgument(languageVersion),
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, klibOutputFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, klibOutputFile.nameWithoutExtension,
                    KotlinWasmCompilerArguments::wasmEnableArrayRangeChecks.cliArgument,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    runIf(allowKotlinPackage) {
                        CommonCompilerArguments::allowKotlinPackage.cliArgument
                    }
                ),
                sources.filter { it.name.endsWith(".kt") }.map { it.absolutePath },
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument,
                        regularAndFriendDependencies.joinToString(File.pathSeparator),
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(CommonJsAndWasmCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator)))
                },
                returnValueCheckerModes.map {
                    CommonCompilerArguments::returnValueChecker.cliArgument(it.state)
                },
                customLanguageFeatures
                    .map { CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":$it" },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
            )
        }
        if (exitCode != ExitCode.OK) {
            val outputStr = compilerXmlOutput.toString(Charsets.UTF_8.name())
            throw CustomKlibCompilerException(exitCode, outputStr)
        }
        return exitCode
    }
}
