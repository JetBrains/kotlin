/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.RETURN_VALUE_CHECKER_MODE
import org.jetbrains.kotlin.test.klib.CustomKlibCompilerException
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Compiles Kotlin sources into a KLIB by invoking the current K/JS CLI compiler ([K2JSCompiler]).
 *
 * Used by [JsGroupingSecondStageFacade] to turn the synthesized batch launcher source into a small `launcher.klib`
 * before linking it together with the per-test KLIBs of the batch. The JVM-side second-stage facades cannot be reused
 * for this: they consume a KLIB, whereas the launcher only exists as generated source at that point.
 *
 * The K/JS counterpart of [org.jetbrains.kotlin.wasm.test.converters.WasmFirstStageInvoker].
 */
class JsFirstStageInvoker(
    val testServices: TestServices,
) {
    private fun callCompiler(output: PrintStream, vararg args: List<String>?): ExitCode {
        val allArgs = args.flatMap { it.orEmpty() }.toTypedArray()
        return K2JSCompiler().execFullPathsInMessages(output, allArgs)
    }

    /**
     * Compiles [sources] into [klibOutputFile].
     *
     * Without `-Xinclude` the K/JS CLI stops after KLIB serialization, which is exactly the first stage we need here.
     */
    fun compileSourcesToKlib(
        module: TestModule,
        sources: List<File>,
        klibOutputFile: File,
        languageVersion: LanguageVersion,
        customOptIns: List<String>,
        allowKotlinPackage: Boolean,
        regularDependencies: Set<String>,
        friendDependencies: Set<String>,
    ) {
        val returnValueCheckerModes: List<ReturnValueCheckerMode> = module.directives[RETURN_VALUE_CHECKER_MODE]
        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            val regularAndFriendDependencies = regularDependencies + friendDependencies
            callCompiler(
                output = printStream,
                listOf(
                    CommonCompilerArguments::languageVersion.cliArgument(languageVersion.versionString),
                    CommonJsAndWasmCompilerArguments::outputDir.cliArgument, klibOutputFile.parentFile.path,
                    CommonJsAndWasmCompilerArguments::moduleName.cliArgument, klibOutputFile.nameWithoutExtension,
                    CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument,
                    CommonCompilerArguments::skipPrereleaseCheck.cliArgument,
                ),
                runIf(allowKotlinPackage) {
                    listOf(CommonCompilerArguments::allowKotlinPackage.cliArgument)
                },
                sources.filter { it.name.endsWith(".kt") }.map { it.absolutePath },
                runIf(regularAndFriendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::libraries.cliArgument(
                            regularAndFriendDependencies.joinToString(File.pathSeparator)
                        )
                    )
                },
                runIf(friendDependencies.isNotEmpty()) {
                    listOf(
                        CommonJsAndWasmCompilerArguments::friendModules.cliArgument(
                            friendDependencies.joinToString(File.pathSeparator)
                        )
                    )
                },
                returnValueCheckerModes.map {
                    CommonCompilerArguments::returnValueChecker.cliArgument(it.state)
                },
                customOptIns.map { CommonCompilerArguments::optIn.cliArgument + "=$it" },
            )
        }
        if (exitCode != ExitCode.OK) {
            throw CustomKlibCompilerException(exitCode, compilerXmlOutput.toString(Charsets.UTF_8.name()))
        }
    }
}
