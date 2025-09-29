/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.forcesPreReleaseBinariesIfEnabled
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.konan.test.blackbox.toOutput
import org.jetbrains.kotlin.konan.test.klib.KlibCrossCompilationOutputTest.Companion.DEPRECATED_K1_LANGUAGE_VERSIONS_DIAGNOSTIC_REGEX
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.*

private const val TEST_DATA_ROOT = "native/native.tests/testData/klib/cross-compilation/manifest-writing"

/**
 * Testdata:
 *  - manifest   - expected klib manifest to be generated
 *  - output.txt - expected compiler output
 *
 * [doManifestTest] runs compiler with designated arguments on a stub-file and asserts that
 * the generated manifest and compiler output correspond to the respective golden files in test data.
 */
@Tag("klib")
@TestDataPath("\$PROJECT_ROOT/$TEST_DATA_ROOT")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class ManifestWritingTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata("simpleManifest")
    fun testSimpleManifest(testInfo: TestInfo) {
        doManifestTest(testInfo)
    }

    @Test
    @TestMetadata("nativeTargetsOverwrite")
    fun testNativeTargetsOverwrite(testInfo: TestInfo) {
        doManifestTest(
            testInfo,
            // note some leading and trailing spaces
            "-Xmanifest-native-targets=ios_arm64, ios_x64,linux_x64 ,mingw_x64,macos_x64,macos_arm64"
        )
    }

    @Test
    @TestMetadata("nativeTargetsOverwriteUnknownTarget")
    fun testNativeTargetsOverwriteUnknownTargetName(testInfo: TestInfo) {
        doManifestTest(
            testInfo,
            "-Xmanifest-native-targets=ios_arm64,ios_x64, unknown_target"
        )
    }

    @Test
    fun testEnableAndDisableLanguageFeatures() {
        val poisoningFeature = LanguageFeature.entries.first { it.forcesPreReleaseBinariesIfEnabled() }
        val enabledLanguageFeature = LanguageFeature.entries.first { it.sinceVersion == LanguageVersion.FIRST_SUPPORTED }

        val compilationResult = compileLibrary(
            testRunSettings,
            stubSourceFile,
            packed = false,
            freeCompilerArgs = listOf("-XXLanguage:+$poisoningFeature", "-XXLanguage:-$enabledLanguageFeature")
        )

        val klib = compilationResult.assertSuccess().resultingArtifact.klibFile
        val manifestFile = File(klib, "default/manifest")
        val manifestProperties = manifestFile.bufferedReader().use { reader -> Properties().apply { load(reader) } }

        checkPropertyAndValue(
            manifestProperties,
            KLIB_PROPERTY_MANUALLY_ALTERED_LANGUAGE_FEATURES,
            poisoningFeature.name,
            enabledLanguageFeature.name
        )
        checkPropertyAndValue(manifestProperties, KLIB_PROPERTY_MANUALLY_ENABLED_POISONING_LANGUAGE_FEATURES, poisoningFeature.name, null)
    }

    private fun doManifestTest(testInfo: TestInfo, vararg additionalCompilerArguments: String) {
        val testName = testInfo.testMethod.get().annotations.firstIsInstance<TestMetadata>().value
        val rootDir = File(TEST_DATA_ROOT, testName)
        require(rootDir.exists()) { "File doesn't exist: ${rootDir.absolutePath}" }

        val compilationResult = compileLibrary(
            testRunSettings,
            stubSourceFile,
            packed = false,
            freeCompilerArgs = additionalCompilerArguments.toList()
        )

        val expectedOutput = rootDir.resolve("output.txt")
        TestDataAssertions.assertEqualsToFile(expectedOutput, compilationResult.toOutput().sanitizeCompilationOutput())

        compareManifests(compilationResult, rootDir.resolve("manifest"))
    }

    fun String.sanitizeCompilationOutput(): String = lines().joinToString(separator = "\n") { line ->
        when {
            DEPRECATED_K1_LANGUAGE_VERSIONS_DIAGNOSTIC_REGEX.matches(line) -> ""
            else -> line
        }
    }

    companion object {


        private fun AbstractNativeSimpleTest.compareManifests(
            compilationResult: TestCompilationResult<out TestCompilationArtifact.KLIB>,
            expectedManifest: File,
        ) {
            val klibRoot = compilationResult.assertSuccess().resultingArtifact
            val actualManifestSanitizedText = readManifestAndSanitize(klibRoot.klibFile, targets.testTarget)

            TestDataAssertions.assertEqualsToFile(expectedManifest, actualManifestSanitizedText)
        }


        private fun checkPropertyAndValue(
            properties: Properties,
            propertyName: String,
            expectedPositiveValue: String?,
            expectedNegativeValue: String?,
        ) {
            val propertyValues = properties.propertyList(propertyName)
            val (positiveValues, negativeValues) = propertyValues.partition { it.startsWith("+") }
            JUnit5Assertions.assertEquals(
                setOfNotNull(expectedPositiveValue),
                positiveValues.map { it.trimStart('+') }.toSet()
            ) { "Property $propertyName should contain positive $expectedPositiveValue, but contains $positiveValues" }
            JUnit5Assertions.assertEquals(
                setOfNotNull(expectedNegativeValue),
                negativeValues.map { it.trimStart('-') }.toSet()
            ) { "Property $propertyName should contain negative $expectedNegativeValue, but contains $negativeValues" }
        }

        private val stubSourceFile: File
            get() = File("$TEST_DATA_ROOT/stub.kt").also {
                require(it.exists()) { "Missing stub.kt-file, looked at: ${it.absolutePath}" }
            }
    }
}