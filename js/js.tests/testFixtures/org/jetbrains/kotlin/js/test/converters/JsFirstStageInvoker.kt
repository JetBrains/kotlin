/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.converters

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Compiles Kotlin sources into a KLIB by invoking the current version of the K/JS CLI compiler.
 *
 * It is needed by the second-stage grouping facades, which compile the synthesized launcher of a grouped batch into
 * a small KLIB before linking it together with the KLIBs of the tests.
 */
object JsFirstStageInvoker {
    /**
     * @param languageVersion The language version to compile with, or `null` for the default one. A version below
     *   the latest stable one also makes the KLIB be exported in the ABI of that version, so a compiler of that
     *   version can link it.
     */
    fun compileSourcesToKlib(
        sources: List<File>,
        klibOutputFile: File,
        libraries: Collection<String>,
        languageVersion: LanguageVersion? = null,
        additionalArguments: List<String> = emptyList(),
    ) {
        val compilerOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerOutput).use { printStream ->
            val arguments = buildList {
                @Suppress("DEPRECATION")
                add(K2JSCompilerArguments::irProduceKlibFile.cliArgument)
                add(K2JSCompilerArguments::outputDir.cliArgument)
                add(klibOutputFile.parentFile.path)
                add(K2JSCompilerArguments::moduleName.cliArgument)
                add(klibOutputFile.nameWithoutExtension)
                add(CommonCompilerArguments::disableDefaultScriptingPlugin.cliArgument)
                add(CommonCompilerArguments::skipPrereleaseCheck.cliArgument)
                if (languageVersion != null) {
                    add(CommonCompilerArguments::languageVersion.cliArgument(languageVersion.versionString))
                    if (languageVersion < LanguageVersion.LATEST_STABLE) {
                        add(CommonCompilerArguments::manuallyConfiguredFeatures.cliArgument + ":+${LanguageFeature.ExportKlibToOlderAbiVersion.name}")
                    }
                }
                if (libraries.isNotEmpty()) {
                    add(K2JSCompilerArguments::libraries.cliArgument)
                    add(libraries.joinToString(File.pathSeparator))
                }
                addAll(additionalArguments)
                sources.mapTo(this) { it.absolutePath }
            }
            K2JSCompiler().execFullPathsInMessages(printStream, arguments.toTypedArray())
        }
        if (exitCode != ExitCode.OK) {
            throw AssertionError(
                "Compilation of ${sources.joinToString { it.name }} into a KLIB finished with $exitCode:\n" +
                        compilerOutput.toString(Charsets.UTF_8.name())
            )
        }
    }
}
