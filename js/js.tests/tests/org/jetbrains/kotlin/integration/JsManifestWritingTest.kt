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
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths.KOTLIN_JS_STDLIB_KLIB_PATH
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.forcesPreReleaseBinariesIfEnabled
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_MANUALLY_ALTERED_LANGUAGE_FEATURES
import org.jetbrains.kotlin.library.KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import java.io.File
import java.util.*
import kotlin.test.assertContains

private val foo = TestKtFile("foo.kt", "fun foo() = 42")

class JsManifestWritingTest : TestCaseWithTmpdir() {
    private val jsStdlib: String?
        get() = System.getProperty(KOTLIN_JS_STDLIB_KLIB_PATH)
    private val outKlibDir: String
        get() = tmpdir.resolve("out").absolutePath

    fun testEnableAndDisableLanguageFeatures() {
        jsStdlib?.let { lib ->
            val poisoningFeature =
                LanguageFeature.entries.first { it.forcesPreReleaseBinariesIfEnabled() }
            val enabledLanguageFeature = LanguageFeature.entries.first { it.sinceVersion == LanguageVersion.FIRST_SUPPORTED }

            runCompiler(
                K2JSCompiler(),
                foo,
                lib,
                outKlibDir,
                listOf("-XXLanguage:+${poisoningFeature}", "-XXLanguage:-${enabledLanguageFeature}"),
            )

            val manifestFile = File(outKlibDir, "default/manifest")
            val manifestProperties = manifestFile.bufferedReader().use { reader -> Properties().apply { load(reader) } }

            checkPropertyAndValue(
                manifestProperties,
                KLIB_PROPERTY_MANUALLY_ALTERED_LANGUAGE_FEATURES,
                poisoningFeature.name,
                enabledLanguageFeature.name
            )
            checkPropertyAndValue(manifestProperties, KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES, poisoningFeature.name, null)
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
        private fun checkPropertyAndValue(
            properties: Properties,
            propertyName: String,
            expectedPositiveValue: String?,
            expectedNegativeValue: String?,
        ) {
            val propertyValues = properties.propertyList(propertyName)
            val (positiveValues, negativeValues) = propertyValues.partition { it.startsWith("+") }
            // The assert checks for conclusion rather than equality due to the presence of an extra feature in testing environment:
            // JsAllowValueClassesInExternals
            assertContains(
                positiveValues.map { it.trimStart('+') },
                expectedPositiveValue,
                "Property $propertyName should contain positive $expectedPositiveValue"
            )
            JUnit5Assertions.assertEquals(
                setOfNotNull(expectedNegativeValue),
                negativeValues.map { it.trimStart('-') }.toSet()
            ) { "Property $propertyName should contain negative $expectedNegativeValue, but contains $negativeValues" }
        }
    }
}
