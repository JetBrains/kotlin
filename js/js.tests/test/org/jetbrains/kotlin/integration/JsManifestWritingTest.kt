/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.integration.JsCompilationTestHelper.TestKtFile
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ENABLED_LANGUAGE_FEATURES
import org.jetbrains.kotlin.library.KLIB_PROPERTY_SUPPRESSED_LANGUAGE_FEATURES
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File

private val foo = TestKtFile("foo.kt", "fun foo() = 42")

class JsManifestWritingTest : TestCaseWithTmpdir() {
    private val jsStdlib: String?
        get() = System.getProperty("kotlin.js.full.stdlib.path")
    private val outKlibDir: String
        get() = tmpdir.resolve("out").absolutePath

    fun testEnableAndSuppressLanguageFeatures() {
        jsStdlib?.let { lib ->
            val experimentalLanguageFeature = LanguageFeature.entries.first { it.sinceVersion == null }
            val enabledLanguageFeature = LanguageFeature.entries.first { it.sinceVersion == LanguageVersion.LATEST_STABLE }

            runCompiler(
                K2JSCompiler(),
                foo,
                lib,
                outKlibDir,
                listOf("-XXLanguage:+${experimentalLanguageFeature}", "-XXLanguage:-${enabledLanguageFeature}"),
            )
            val manifestProperties = File(outKlibDir, "default/manifest").readText().split("\n")

            checkPropertyAndValue(manifestProperties, KLIB_PROPERTY_ENABLED_LANGUAGE_FEATURES, experimentalLanguageFeature.name)
            checkPropertyAndValue(manifestProperties, KLIB_PROPERTY_SUPPRESSED_LANGUAGE_FEATURES, enabledLanguageFeature.name)

        }
    }

    private fun runCompiler(
        compiler: CLICompiler<*>,
        src: TestKtFile,
        libs: String,
        outFile: String,
        extras: List<String> = emptyList(),
        messageRenderer: MessageRenderer? = null,
    ) {
        val mainKt = tmpdir.resolve(src.name).apply { writeText(src.content) }
        val outputFile = File(outFile)
        val args = listOf(
            mainKt.absolutePath,
            K2JSCompilerArguments::libraries.cliArgument(libs),
            K2JSCompilerArguments::outputDir.cliArgument(outputFile.path),
            K2JSCompilerArguments::moduleName.cliArgument(outputFile.nameWithoutExtension),
            K2JSCompilerArguments::languageVersion.cliArgument(LanguageVersion.LATEST_STABLE.versionString),
            K2JSCompilerArguments::irProduceKlibDir.cliArgument,
        )
        CompilerTestUtil.executeCompilerAssertSuccessful(compiler, args + extras, messageRenderer)
    }

    companion object {
        private fun checkPropertyAndValue(properties: List<String>, propertyName: String, propertyValue: String) {
            val property = properties.firstOrNull { it.startsWith(propertyName) }
            assertNotNull(property, "Property $propertyName not found in the manifest")
            assertTrue(property!!.contains(propertyValue))
        }
    }
}