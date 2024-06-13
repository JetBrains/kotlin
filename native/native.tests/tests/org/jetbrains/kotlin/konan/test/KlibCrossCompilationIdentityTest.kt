/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.compileLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeClassLoader
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpIr
import org.jetbrains.kotlin.konan.test.blackbox.support.util.dumpMetadata
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.absolute

private const val TEST_DATA_ROOT = "native/native.tests/testData/klib/crossCompilationIdentity"

/**
 * This test works in the following way:
 *
 * - a klib for darwin target is compiled from sources
 * - a special checksum is computed
 * - checksum is compared against the golden-file
 *
 * The main idea is that the test is launched on all hosts (Linux, Macos, Win) and therefore
 * indirectly asserts that the generated klib is "identical" across these hosts
 */
@TestDataPath("\$PROJECT_ROOT/$TEST_DATA_ROOT")
abstract class KlibCrossCompilationIdentityTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata("simpleSmoke.kt")
    fun testSimpleSmoke(testInfo: TestInfo) {
        doCrossCompilationIdentityTest(testInfo)
    }

    @Test
    @TestMetadata("stdlibReferences.kt")
    fun testStdlibReferences(testInfo: TestInfo) {
        doCrossCompilationIdentityTest(testInfo)
    }

    @Test
    @TestMetadata("simpleReferenceToDarwinApi.kt")
    fun testSimpleReferenceToDarwinApi(testInfo: TestInfo) {
        doCrossCompilationIdentityTest(testInfo)
    }

    private fun doCrossCompilationIdentityTest(testInfo: TestInfo) {
        Assumptions.assumeTrue(isCrossDistAvailable())
        val testName = testInfo.testMethod.get().name.toString()
            .let { if (it.startsWith("test")) it.removePrefix("test") else it }
            .decapitalizeAsciiOnly()

        val testData = File(TEST_DATA_ROOT, "$testName.kt")
        require(testData.exists()) {
            """
                Can't find test data corresponding to the test!
                testMethod = ${testInfo.testMethod.get().name}
                sanitized test name = $testName
                expected testDataDir = ${testData.canonicalPath}
            """.trimIndent()
        }
        val testDataDir = File(testData.parent)

        val compilationResult = compileLibrary(
            testRunSettings,
            testData,
            packed = false,
            freeCompilerArgs = listOf("-Xklib-relative-path-base=${testDataDir.canonicalPath}")
        ).assertSuccess()
        val klib: TestCompilationArtifact.KLIB = compilationResult.resultingArtifact

        val metadataDump = klib.dumpMetadata(
            testRunSettings.get<KotlinNativeClassLoader>().classLoader,
            printSignatures = false,
            signatureVersion = null,
        )
        KotlinTestUtils.assertEqualsToFile(File(testDataDir, "$testName.metadata.txt"), metadataDump)

        val irDump = klib.dumpIr(
            testRunSettings.get<KotlinNativeClassLoader>().classLoader,
            printSignatures = true,
            signatureVersion = null
        )
        KotlinTestUtils.assertEqualsToFile(File(testDataDir, "$testName.ir.txt"), irDump)

        with(ManifestWritingTest) {
            compareManifests(compilationResult, testDataDir.resolve("$testName.manifest"))
        }

        val defaultFolder = klib.klibFile.toPath().resolve("default")
        val irFolder = defaultFolder.resolve("ir")
        val linkdataFolder = defaultFolder.resolve("linkdata")
        val irMd5 = irFolder.computeMd5()
        val linkdataMd5 = linkdataFolder.computeMd5()

        // Ideally, text dumps should change iff md5 changes. But there's a concern that dumping algorithm
        // might miss some cases. So, these hashes are computed as a "safety net"
        // If you arrived here because the md5-comparison failed after you changes, and text-dumps didn't change,
        // please investigate if it's possible to represent the IR/linkdata diff caught by md5 digest in .txt-dumps
        KotlinTestUtils.assertEqualsToFile(File(testDataDir, "$testName.ir.md5.txt"), irMd5)
        KotlinTestUtils.assertEqualsToFile(File(testDataDir, "$testName.metadata.md5.txt"), linkdataMd5)
    }


    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        private fun Path.computeMd5(): String {
            val base = toFile()
            require(base.exists()) { "File doesn't exist: ${absolute()}" }
            val md = MessageDigest.getInstance("MD5")
            base.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.relativeTo(base) }
                .forEach { md.update(it.readBytes()) }

            return md.digest().toHexString()
        }

        private fun isCrossDistAvailable(): Boolean =
            HostManager.hostIsMac || System.getProperty(FULL_CROSS_DIST_ENABLED_PROPERTY)?.toBoolean() ?: false

        // If you rename/change it, adjust native/native.tests/build.gradle.kts as well
        private const val FULL_CROSS_DIST_ENABLED_PROPERTY = "kotlin.native.internal.fullCrossDistEnabled"
    }
}

@FirPipeline
@Tag("frontend-fir")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.TEST_TARGET, "ios_arm64")
class FirKlibCrossCompilationIdentityTest : KlibCrossCompilationIdentityTest()
