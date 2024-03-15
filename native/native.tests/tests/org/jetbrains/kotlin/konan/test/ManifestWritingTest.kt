/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.konan.test.blackbox.toOutput
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

/**
 * Testdata:
 *  - manifest   - expected klib manifest to be generated
 *  - output.txt - expected compiler output
 *
 * [doManifestTest] runs compiler with designated arguments on a stub-file and asserts that
 * the generated manifest and compiler output correspond to the respective golden files in test data.
 */
@TestDataPath("\$PROJECT_ROOT/native/native.tests/testData/klib/manifestWriting")
abstract class ManifestWritingTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata("simpleManifest")
    fun testSimpleManifest() {
        doManifestTest("native/native.tests/testData/klib/manifestWriting/simpleManifest")
    }

    @Test
    @TestMetadata("nativeTargetsOverwrite")
    fun testNativeTargetsOverwrite() {
        doManifestTest(
            "native/native.tests/testData/klib/manifestWriting/nativeTargetsOverwrite",
            // note some leading and trailing spaces
            "-Xmanifest-native-targets=ios_arm64, ios_x64,linux_x64 ,mingw_x64,macos_x64,macos_arm64"
        )
    }

    @Test
    @TestMetadata("nativeTargetsOverwriteUnknownTarget")
    fun testNativeTargetsOverwriteUnknownTargetName() {
        doManifestTest(
            "native/native.tests/testData/klib/manifestWriting/nativeTargetsOverwriteUnknownTarget",
            "-Xmanifest-native-targets=ios_arm64,ios_x64, unknown_target"
        )
    }

    private fun doManifestTest(testDataDir: String, vararg additionalCompilerArguments: String) {
        val rootDir = File(testDataDir)
        require(rootDir.exists()) { "File doesn't exist: ${rootDir.absolutePath}" }

        val compilationResult = compileLibrary(
            testRunSettings,
            stubSourceFile,
            packed = false,
            freeCompilerArgs = additionalCompilerArguments.toList()
        )

        val expectedOutput = rootDir.resolve("output.txt")
        KotlinTestUtils.assertEqualsToFile(expectedOutput, compilationResult.toOutput())

        val klibRoot = compilationResult.assertSuccess().resultingArtifact
        val actualManifest = klibRoot.klibFile.resolve("default/manifest")
        assert(actualManifest.exists())

        val actualManifestSanitized = sanitizeManifest(Properties().apply { load(actualManifest.reader()) }, targets)
        val actualManifestSanitizedText = actualManifestSanitized.joinToString(separator = "\n") { (key, value) -> "$key = $value" }

        val expectedManifest = rootDir.resolve("manifest")
        KotlinTestUtils.assertEqualsToFile(expectedManifest, actualManifestSanitizedText)
    }

    companion object {
        private val TRANSIENT_MANIFEST_PROPERTIES = listOf(
            KLIB_PROPERTY_ABI_VERSION,
            KLIB_PROPERTY_METADATA_VERSION,
            KLIB_PROPERTY_LIBRARY_VERSION,
            KLIB_PROPERTY_COMPILER_VERSION,
            KLIB_PROPERTY_IR_SIGNATURE_VERSIONS
        )

        private const val SANITIZED_VALUE_STUB = "<value sanitized for test data stability>"
        private const val SANITIZED_TEST_RUN_TARGET = "<test-run-target>"

        private fun sanitizeManifest(original: Properties, testRunTargets: KotlinNativeTargets): List<Pair<String, String>> {
            // intentionally not using Properties as output to guarantee stable order of properties
            val result = mutableListOf<Pair<String, String>>()
            original.entries.forEach {
                val key = it.key as String
                val value = it.value as String

                val sanitizedValue = when (key) {
                    in TRANSIENT_MANIFEST_PROPERTIES -> SANITIZED_VALUE_STUB

                    KLIB_PROPERTY_NATIVE_TARGETS -> {
                        val targets = value.split(" ")
                        val singleTarget = targets.singleOrNull()
                        if (singleTarget == testRunTargets.testTarget.name) SANITIZED_TEST_RUN_TARGET else value
                    }

                    else -> value
                }
                result += key to sanitizedValue
            }

            return result.sortedBy { it.first }
        }

        private val stubSourceFile: File
            get() = File("native/native.tests/testData/klib/manifestWriting/stub.kt").also {
                require(it.exists()) { "Missing stub.kt-file, looked at: ${it.absolutePath}" }
            }
    }
}

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class ClassicFEManifestWritingTest : ManifestWritingTest()

@FirPipeline
@Tag("frontend-fir")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class FirFEManifestWritingTest : ManifestWritingTest() {
}
