/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.backend.common.diagnostics.StandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.backend.common.diagnostics.StandardLibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_LIBRARY_VERSION
import org.jetbrains.kotlin.backend.common.diagnostics.StandardLibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_MANIFEST_FILE
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File
import java.util.*
import java.util.jar.Manifest
import org.jetbrains.kotlin.konan.file.File as KFile

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
                expectedWarningStatus = WarningStatus.JS_OLD_STDLIB_WARNING
            )
        }
    }

    fun testJsOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            val sameLanguageVersion = haveSameLanguageVersion(currentVersion, nextVersion)
            compileDummyLibrary(
                stdlibVersion = nextVersion,
                compilerVersion = currentVersion,
                isWasm = false,
                expectedWarningStatus = if (sameLanguageVersion) WarningStatus.NO_WARNINGS else WarningStatus.JS_TOO_NEW_STDLIB_WARNING
            )
        }
    }

    fun testWasmMismatchingVersions() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            for ((stdlibVersion, compilerVersion) in listOf(currentVersion to nextVersion, nextVersion to currentVersion)) {
                compileDummyLibrary(
                    stdlibVersion = stdlibVersion,
                    compilerVersion = compilerVersion,
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
        compileDummyLibrary(stdlibVersion, compilerVersion, isWasm, isZipped = false, expectedWarningStatus)
        compileDummyLibrary(stdlibVersion, compilerVersion, isWasm, isZipped = true, expectedWarningStatus)
    }

    private fun compileDummyLibrary(
        stdlibVersion: TestVersion?,
        compilerVersion: TestVersion?,
        isWasm: Boolean,
        isZipped: Boolean,
        expectedWarningStatus: WarningStatus,
    ) {
        val sourcesDir = createDir("sources")
        val outputDir = createDir("build")

        val sourceFile = sourcesDir.resolve("file.kt").apply { writeText("fun foo() = 42\n") }
        val moduleName = getTestName(true)

        val messageCollector = MessageCollectorImpl()

        withCustomCompilerVersion(compilerVersion) {
            val fakeStdlib = if (isZipped)
                createFakeZippedStdlibWithSpecificVersion(isWasm, stdlibVersion)
            else
                createFakeUnzippedStdlibWithSpecificVersion(isWasm, stdlibVersion)

            runJsCompiler(messageCollector) {
                this.freeArgs = listOf(sourceFile.absolutePath)
                this.libraries = fakeStdlib.absolutePath
                this.outputDir = outputDir.absolutePath
                this.moduleName = moduleName
                this.irProduceKlibFile = true
                this.irModuleName = moduleName
                this.wasm = isWasm
            }
        }

        val success = when (expectedWarningStatus) {
            WarningStatus.NO_WARNINGS -> !messageCollector.hasJsOldStdlibWarning() && !messageCollector.hasJsTooNewStdlibWarning() && !messageCollector.hasWasmWarning()
            WarningStatus.JS_OLD_STDLIB_WARNING -> messageCollector.hasJsOldStdlibWarning(stdlibVersion!! to compilerVersion!!)
            WarningStatus.JS_TOO_NEW_STDLIB_WARNING -> messageCollector.hasJsTooNewStdlibWarning(stdlibVersion!! to compilerVersion!!)
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

    private fun MessageCollectorImpl.hasJsOldStdlibWarning(
        specificVersions: Pair<TestVersion, TestVersion>? = null,
    ): Boolean {
        val stdlibMessagePart = "Kotlin/JS standard library has an older version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "than the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    private fun MessageCollectorImpl.hasJsTooNewStdlibWarning(
        specificVersions: Pair<TestVersion, TestVersion>? = null,
    ): Boolean {
        val stdlibMessagePart = "The Kotlin/JS standard library has a more recent version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "The compiler version is " + specificVersions?.second?.toString().orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    private fun MessageCollectorImpl.hasWasmWarning(
        specificVersions: Pair<TestVersion, TestVersion>? = null,
    ): Boolean {
        val stdlibMessagePart = "The version of the Kotlin/Wasm standard library" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "differs from the version of the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    private fun haveSameLanguageVersion(a: TestVersion, b: TestVersion): Boolean =
        a.basicVersion.major == b.basicVersion.major && a.basicVersion.minor == b.basicVersion.minor

    private fun createFakeUnzippedStdlibWithSpecificVersion(isWasm: Boolean, version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedStdlibDir = createDir("dependencies/stdlib-${rawVersion ?: "unknown"}")
        val manifestFile = patchedStdlibDir.resolve("default").resolve("manifest")
        if (manifestFile.exists()) return patchedStdlibDir

        val originalStdlibDir = File(System.getProperty("kotlin.js.full.stdlib.path"))
        assertTrue(originalStdlibDir.isDirectory)
        originalStdlibDir.copyRecursively(patchedStdlibDir)

        if (rawVersion != null) {
            val jarManifestFile = patchedStdlibDir.resolve(KLIB_JAR_MANIFEST_FILE)
            jarManifestFile.parentFile.mkdirs()
            jarManifestFile.outputStream().use { os ->
                with(Manifest()) {
                    mainAttributes.putValue(KLIB_JAR_LIBRARY_VERSION, rawVersion)
                    mainAttributes.putValue("Manifest-Version", "1.0") // some convention stuff to make Jar manifest work
                    write(os)
                }
            }
        }

        if (isWasm) {
            val properties = manifestFile.inputStream().use { Properties().apply { load(it) } }
            properties[KLIB_PROPERTY_BUILTINS_PLATFORM] = BuiltInsPlatform.WASM.name
            manifestFile.outputStream().use { properties.store(it, null) }
        }

        return patchedStdlibDir
    }

    private fun createFakeZippedStdlibWithSpecificVersion(isWasm: Boolean, version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedStdlibFile = createFile("dependencies/stdlib-${rawVersion ?: "unknown"}.klib")
        if (patchedStdlibFile.exists()) return patchedStdlibFile

        val unzippedStdlibDir = createFakeUnzippedStdlibWithSpecificVersion(isWasm, version)
        zipDirectory(directory = unzippedStdlibDir, zipFile = patchedStdlibFile)

        return patchedStdlibFile
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
    private fun createFile(name: String): File = tmpdir.resolve(name).apply { parentFile.mkdirs() }

    private enum class WarningStatus { NO_WARNINGS, JS_OLD_STDLIB_WARNING, JS_TOO_NEW_STDLIB_WARNING, WASM_WARNING }

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
                TestVersion(1, 8, 24),
                TestVersion(1, 8, 25),
                TestVersion(1, 9, 1),
                TestVersion(2, 0, 0),
                TestVersion(2, 0, 0, "-dev-1234"),
                TestVersion(2, 0, 0, "-dev-4321"),
                TestVersion(2, 0, 0, "-Beta1"),
                TestVersion(2, 0, 0, "-Beta2"),
                TestVersion(2, 0, 255, "-SNAPSHOT"),
            ).groupByTo(TreeMap()) { it.basicVersion }.values.toList()

        private fun zipDirectory(directory: File, zipFile: File) {
            KFile(directory.toPath()).zipDirAs(KFile(zipFile.toPath()))
        }
    }
}
