/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ClassicPipeline
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.konan.test.blackbox.toOutput
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
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
abstract class ManifestWritingTest : AbstractNativeSimpleTest() {
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
        KotlinTestUtils.assertEqualsToFile(expectedOutput, compilationResult.toOutput())

        compareManifests(compilationResult, rootDir.resolve("manifest"))
    }

    companion object {
        private val TRANSIENT_MANIFEST_PROPERTIES = listOf(
            KLIB_PROPERTY_ABI_VERSION,
            KLIB_PROPERTY_METADATA_VERSION,
            KLIB_PROPERTY_COMPILER_VERSION,
            KLIB_PROPERTY_IR_SIGNATURE_VERSIONS
        )

        private const val SANITIZED_VALUE_STUB = "<value sanitized for test data stability>"
        private const val SANITIZED_TEST_RUN_TARGET = "<test-run-target>"

        private fun AbstractNativeSimpleTest.compareManifests(
            compilationResult: TestCompilationResult<out TestCompilationArtifact.KLIB>,
            expectedManifest: File,
        ) {
            val klibRoot = compilationResult.assertSuccess().resultingArtifact
            val actualManifestSanitizedText = readManifestAndSanitize(klibRoot.klibFile, targets.testTarget)

            KotlinTestUtils.assertEqualsToFile(expectedManifest, actualManifestSanitizedText)
        }

        internal fun readManifestAndSanitize(klibDir: File, singleTargetInManifestToBeReplacedByTheAlias: KonanTarget?): String {
            val manifestFile = File(klibDir, "default/manifest")
            assertTrue(manifestFile.exists()) { "File does not exist: $manifestFile" }

            val manifestProperties = manifestFile.bufferedReader().use { reader -> Properties().apply { load(reader) } }
            return sanitizeManifest(manifestProperties, singleTargetInManifestToBeReplacedByTheAlias).joinToString(separator = "\n") { (key, value) -> "$key = $value" }
        }

        private fun sanitizeManifest(original: Properties, singleTargetInManifestToBeReplacedByTheAlias: KonanTarget?): List<Pair<String, String>> {
            // intentionally not using Properties as output to guarantee stable order of properties
            val result = mutableListOf<Pair<String, String>>()
            original.entries.forEach {
                val key = it.key as String
                val value = it.value as String

                val sanitizedValue = when (key) {
                    in TRANSIENT_MANIFEST_PROPERTIES -> SANITIZED_VALUE_STUB

                    KLIB_PROPERTY_NATIVE_TARGETS -> {
                        val singleTargetPresentInManifest = value.split(" ").singleOrNull()
                        if (singleTargetPresentInManifest != null && singleTargetInManifestToBeReplacedByTheAlias != null) {
                            SANITIZED_TEST_RUN_TARGET
                        } else {
                            value
                        }
                    }

                    else -> value
                }
                result += key to sanitizedValue
            }

            return result.sortedBy { it.first }
        }

        private val stubSourceFile: File
            get() = File("$TEST_DATA_ROOT/stub.kt").also {
                require(it.exists()) { "Missing stub.kt-file, looked at: ${it.absolutePath}" }
            }
    }
}

@ClassicPipeline()
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class ClassicFEManifestWritingTest : ManifestWritingTest()

@FirPipeline
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class FirFEManifestWritingTest : ManifestWritingTest() {
}
