/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.backend.common.diagnostics.StandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.utils.TestMessageCollector
import java.io.File
import java.util.*

class JsWasmStdlibSpecialCompatibilityChecksTest : TestCaseWithTmpdir() {

    fun testJsSameBasicCompilerVersion() = testSameBasicCompilerVersion(isWasm = false)

    fun testWasmSameBasicCompilerVersion() = testSameBasicCompilerVersion(isWasm = true)

    private fun testSameBasicCompilerVersion(isWasm: Boolean) {
        for (versionsWithSameBasicVersion in SORTED_TEST_VERSION_GROUPS) {
            for (stdlibVersion in versionsWithSameBasicVersion) {
                for (compilerVersion in versionsWithSameBasicVersion) {
                    compileDummyLibrary(
                        stdlibVersion = stdlibVersion,
                        compilerVersion = compilerVersion,
                        isWasm = isWasm,
                        expectedWarningStatus = WarningStatus.NO_WARNINGS
                    )
                }
            }
        }
    }

    fun testJsNewerCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                stdlibVersion = currentVersion,
                compilerVersion = nextVersion,
                isWasm = false,
                expectedWarningStatus = WarningStatus.JS_WARNING
            )
        }
    }

    fun testJsOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                stdlibVersion = nextVersion,
                compilerVersion = currentVersion,
                isWasm = false,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    fun testWasmMismatchingVersions() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            for ((stdlibVersion, compilerVersion) in listOf(currentVersion to nextVersion, nextVersion to currentVersion)) {
                compileDummyLibrary(
                    stdlibVersion = currentVersion,
                    compilerVersion = nextVersion,
                    isWasm = true,
                    expectedWarningStatus = WarningStatus.WASM_WARNING
                )
            }
        }
    }

    private inline fun testCurrentAndNextBasicVersions(block: (currentVersion: TestVersion, nextVersion: TestVersion) -> Unit) {
        for (i in 0..SORTED_TEST_VERSION_GROUPS.size - 2) {
            val versionsWithSameBasicVersion = SORTED_TEST_VERSION_GROUPS[i]
            val versionsWithNextSameBasicVersion = SORTED_TEST_VERSION_GROUPS[i + 1]

            for (currentVersion in versionsWithSameBasicVersion) {
                for (nextVersion in versionsWithNextSameBasicVersion) {
                    block(currentVersion, nextVersion)
                }
            }
        }
    }

    fun testJsEitherVersionIsMissing() {
        listOf(
            TestVersion(2, 0, 0) to null,
            null to TestVersion(2, 0, 0),
        ).forEach { (stdlibVersion, compilerVersion) ->
            compileDummyLibrary(
                stdlibVersion = stdlibVersion,
                compilerVersion = compilerVersion,
                isWasm = false,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    fun testWasmEitherVersionIsMissing() {
        listOf(
            TestVersion(2, 0, 0) to null,
            null to TestVersion(2, 0, 0),
        ).forEach { (stdlibVersion, compilerVersion) ->
            compileDummyLibrary(
                stdlibVersion = stdlibVersion,
                compilerVersion = compilerVersion,
                isWasm = true,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    private fun compileDummyLibrary(
        stdlibVersion: TestVersion?,
        compilerVersion: TestVersion?,
        isWasm: Boolean,
        expectedWarningStatus: WarningStatus,
    ) {
        val sourcesDir = createDir("sources")
        val outputDir = createDir("build")

        val sourceFile = sourcesDir.resolve("file.kt").apply { writeText("fun foo() = 42\n") }
        val moduleName = getTestName(true)

        val messageCollector = TestMessageCollector()

        withCustomCompilerVersion(compilerVersion) {
            val fakeStdlib = createFakeStdlibWithSpecificVersion(isWasm, stdlibVersion)
            runJsCompiler(messageCollector) {
                this.freeArgs = listOf(sourceFile.absolutePath)
                this.noStdlib = true // it is passed explicitly
                this.libraries = fakeStdlib.absolutePath
                this.outputDir = outputDir.absolutePath
                this.moduleName = moduleName
                this.irProduceKlibFile = true
                this.irOnly = true
                this.irModuleName = moduleName
                this.wasm = isWasm
            }
        }

        val success = when (expectedWarningStatus) {
            WarningStatus.NO_WARNINGS -> !messageCollector.hasJsWarning() && !messageCollector.hasWasmWarning()
            WarningStatus.JS_WARNING -> messageCollector.hasJsWarning(stdlibVersion!! to compilerVersion!!)
            WarningStatus.WASM_WARNING -> messageCollector.hasWasmWarning(stdlibVersion!! to compilerVersion!!)
        }

        if (!success) fail(
            buildString {
                appendLine("Compiling with stdlib=[$stdlibVersion] and compiler=[$compilerVersion]")
                appendLine("Logger compiler messages (${messageCollector.messages.size} items):")
                messageCollector.messages.joinTo(this, "\n")
            }
        )
    }

    private fun TestMessageCollector.hasJsWarning(
        specificVersions: Pair<TestVersion, TestVersion>? = null,
    ): Boolean {
        val stdlibMessagePart = "Kotlin/JS standard library has an older version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "than the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    private fun TestMessageCollector.hasWasmWarning(
        specificVersions: Pair<TestVersion, TestVersion>? = null,
    ): Boolean {
        val stdlibMessagePart = "The version of the Kotlin/Wasm standard library" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "differs from the version of the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    private fun createFakeStdlibWithSpecificVersion(isWasm: Boolean, version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedStdlibDir = createDir("dependencies/stdlib-${rawVersion ?: "unknown"}")
        val manifestFile = patchedStdlibDir.resolve("default").resolve("manifest")
        if (manifestFile.exists()) return patchedStdlibDir

        val originalStdlibDir = File(System.getProperty("kotlin.js.full.stdlib.path"))
        assertTrue(originalStdlibDir.isDirectory)
        originalStdlibDir.copyRecursively(patchedStdlibDir)

        val properties = manifestFile.inputStream().use { Properties().apply { load(it) } }
        properties.remove(KLIB_PROPERTY_COMPILER_VERSION)
        if (rawVersion != null) properties[KLIB_PROPERTY_COMPILER_VERSION] = rawVersion

        if (isWasm) {
            properties[KLIB_PROPERTY_BUILTINS_PLATFORM] = BuiltInsPlatform.WASM.name
        }

        manifestFile.outputStream().use { properties.store(it, null) }

        return patchedStdlibDir
    }

    private inline fun <T> withCustomCompilerVersion(version: TestVersion?, block: () -> T): T {
        @Suppress("DEPRECATION")
        return try {
            StandardLibrarySpecialCompatibilityChecker.setUpCustomCompilerVersionForTest(version?.toString())
            block()
        } finally {
            StandardLibrarySpecialCompatibilityChecker.resetUpCustomCompilerVersionForTest()
        }
    }

    private fun createDir(name: String): File = tmpdir.resolve(name).apply { mkdirs() }

    private enum class WarningStatus { NO_WARNINGS, JS_WARNING, WASM_WARNING }

    private class TestVersion(val basicVersion: KotlinVersion, val postfix: String) : Comparable<TestVersion> {
        constructor(major: Int, minor: Int, patch: Int, postfix: String = "") : this(KotlinVersion(major, minor, patch), postfix)

        override fun compareTo(other: TestVersion) = basicVersion.compareTo(other.basicVersion)
        override fun equals(other: Any?) = (other as? TestVersion)?.basicVersion == basicVersion
        override fun hashCode() = basicVersion.hashCode()
        override fun toString() = basicVersion.toString() + postfix
    }

    companion object {
        private val SORTED_TEST_VERSION_GROUPS: List<Collection<TestVersion>> =
            listOf(
                TestVersion(1, 9, 24),
                TestVersion(1, 9, 25),
                TestVersion(1, 10, 1),
                TestVersion(2, 0, 0),
                TestVersion(2, 0, 0, "-dev-1234"),
                TestVersion(2, 0, 0, "-dev-4321"),
                TestVersion(2, 0, 0, "-Beta1"),
                TestVersion(2, 0, 0, "-Beta2"),
                TestVersion(2, 0, 255, "-SNAPSHOT"),
            ).groupByTo(TreeMap()) { it.basicVersion }.values.toList()
    }
}
