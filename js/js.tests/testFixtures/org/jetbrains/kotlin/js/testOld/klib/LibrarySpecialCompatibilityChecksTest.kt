/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_LIBRARY_VERSION
import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_MANIFEST_FILE
import org.jetbrains.kotlin.cli.common.ExitCode
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

abstract class LibrarySpecialCompatibilityChecksTest : TestCaseWithTmpdir() {

    fun testSameBasicCompilerVersion() {
        for (versionsWithSameBasicVersion in SORTED_TEST_VERSION_GROUPS) {
            for (libraryVersion in versionsWithSameBasicVersion) {
                for (compilerVersion in versionsWithSameBasicVersion) {
                    for (isWasm in listOf(false, true)) {
                        compileDummyLibrary(
                            libraryVersion = libraryVersion,
                            compilerVersion = compilerVersion,
                            isWasm = isWasm,
                            expectedWarningStatus = WarningStatus.NO_WARNINGS
                        )
                    }
                }
            }
        }
    }

    fun testJsNewerCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                libraryVersion = currentVersion,
                compilerVersion = nextVersion,
                isWasm = false,
                expectedWarningStatus = WarningStatus.JS_OLD_LIBRARY_WARNING
            )
        }
    }

    fun testJsOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            val sameLanguageVersion = haveSameLanguageVersion(currentVersion, nextVersion)
            compileDummyLibrary(
                libraryVersion = nextVersion,
                compilerVersion = currentVersion,
                isWasm = false,
                expectedWarningStatus = if (sameLanguageVersion) WarningStatus.NO_WARNINGS else WarningStatus.JS_TOO_NEW_LIBRARY_WARNING
            )
        }
    }

    fun testWasmNewerCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                libraryVersion = currentVersion,
                compilerVersion = nextVersion,
                isWasm = true,
                expectedWarningStatus = WarningStatus.WASM_OLD_LIBRARY_WARNING
            )
        }
    }

    fun testWasmOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            val sameLanguageVersion = haveSameLanguageVersion(currentVersion, nextVersion)
            compileDummyLibrary(
                libraryVersion = nextVersion,
                compilerVersion = currentVersion,
                isWasm = true,
                expectedWarningStatus = if (sameLanguageVersion) WarningStatus.NO_WARNINGS else WarningStatus.WASM_TOO_NEW_LIBRARY_WARNING
            )
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
        ).forEach { (libraryVersion, compilerVersion) ->
            compileDummyLibrary(
                libraryVersion = libraryVersion,
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
        ).forEach { (libraryVersion, compilerVersion) ->
            compileDummyLibrary(
                libraryVersion = libraryVersion,
                compilerVersion = compilerVersion,
                isWasm = true,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    private fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        isWasm: Boolean,
        expectedWarningStatus: WarningStatus,
    ) {
        compileDummyLibrary(libraryVersion, compilerVersion, isWasm, isZipped = false, expectedWarningStatus)
        compileDummyLibrary(libraryVersion, compilerVersion, isWasm, isZipped = true, expectedWarningStatus)
    }

    protected abstract fun MessageCollectorImpl.hasJsOldLibraryError(specificVersions: Pair<TestVersion, TestVersion>? = null): Boolean
    protected abstract fun MessageCollectorImpl.hasJsTooNewLibraryError(specificVersions: Pair<TestVersion, TestVersion>? = null): Boolean
    protected abstract fun MessageCollectorImpl.hasWasmOldLibraryError(specificVersions: Pair<TestVersion, TestVersion>? = null): Boolean
    protected abstract fun MessageCollectorImpl.hasWasmTooNewLibraryError(specificVersions: Pair<TestVersion, TestVersion>? = null): Boolean

    private fun MessageCollectorImpl.checkMessage(
        expectedWarningStatus: WarningStatus,
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
    ) {
        val success = when (expectedWarningStatus) {
            WarningStatus.NO_WARNINGS -> !hasJsOldLibraryError() && !hasJsTooNewLibraryError() && !hasWasmOldLibraryError() && !hasWasmTooNewLibraryError()
            WarningStatus.JS_OLD_LIBRARY_WARNING -> hasJsOldLibraryError(libraryVersion!! to compilerVersion!!)
            WarningStatus.JS_TOO_NEW_LIBRARY_WARNING -> hasJsTooNewLibraryError(libraryVersion!! to compilerVersion!!)
            WarningStatus.WASM_OLD_LIBRARY_WARNING -> hasWasmOldLibraryError(libraryVersion!! to compilerVersion!!)
            WarningStatus.WASM_TOO_NEW_LIBRARY_WARNING -> hasWasmTooNewLibraryError(libraryVersion!! to compilerVersion!!)
        }
        if (!success) fail(
            buildString {
                appendLine("Compiling with stdlib=[$libraryVersion] and compiler=[$compilerVersion]")
                appendLine("Logger compiler messages (${messages.size} items):")
                messages.joinTo(this, "\n")
            }
        )
    }

    private fun compileDummyLibrary(
        libraryVersion: TestVersion?,
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
            val fakeLibrary = if (isZipped)
                createFakeZippedLibraryWithSpecificVersion(isWasm, libraryVersion)
            else
                createFakeUnzippedLibraryWithSpecificVersion(isWasm, libraryVersion)

            val expectedExitCode = if (expectedWarningStatus == WarningStatus.NO_WARNINGS) ExitCode.OK else ExitCode.COMPILATION_ERROR
            runJsCompiler(messageCollector, expectedExitCode) {
                this.freeArgs = listOf(sourceFile.absolutePath)
                this.libraries = (additionalLibraries(isWasm) + fakeLibrary.absolutePath).joinToString(File.pathSeparator)
                this.outputDir = outputDir.absolutePath
                this.moduleName = moduleName
                this.irProduceKlibFile = true
                this.irModuleName = moduleName
                this.wasm = isWasm
            }
        }

        messageCollector.checkMessage(expectedWarningStatus, libraryVersion, compilerVersion)
    }

    private fun haveSameLanguageVersion(a: TestVersion, b: TestVersion): Boolean =
        a.basicVersion.major == b.basicVersion.major && a.basicVersion.minor == b.basicVersion.minor

    protected abstract val originalLibraryPath: String
    protected open fun additionalLibraries(isWasm: Boolean): List<String> = listOf()

    private fun createFakeUnzippedLibraryWithSpecificVersion(isWasm: Boolean, version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedLibraryDir = createDir("dependencies/fakeLib-${rawVersion ?: "unknown"}-${if (isWasm) "wasm" else "js"}")
        val manifestFile = patchedLibraryDir.resolve("default").resolve("manifest")
        if (manifestFile.exists()) return patchedLibraryDir

        val originalLibraryDir = File(originalLibraryPath)
        assertTrue(originalLibraryDir.isDirectory)
        originalLibraryDir.copyRecursively(patchedLibraryDir)

        if (rawVersion != null) {
            val jarManifestFile = patchedLibraryDir.resolve(KLIB_JAR_MANIFEST_FILE)
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

        return patchedLibraryDir
    }

    private fun createFakeZippedLibraryWithSpecificVersion(isWasm: Boolean, version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedLibraryFile = createFile("dependencies/fakeLib-${rawVersion ?: "unknown"}-${if (isWasm) "wasm" else "js"}.klib")
        if (patchedLibraryFile.exists()) return patchedLibraryFile

        val unzippedLibraryDir = createFakeUnzippedLibraryWithSpecificVersion(isWasm, version)
        zipDirectory(directory = unzippedLibraryDir, zipFile = patchedLibraryFile)

        return patchedLibraryFile
    }

    private inline fun <T> withCustomCompilerVersion(version: TestVersion?, block: () -> T): T {
        @Suppress("DEPRECATION")
        return try {
            LibrarySpecialCompatibilityChecker.setUpCustomCompilerVersionForTest(version?.toString())
            block()
        } finally {
            LibrarySpecialCompatibilityChecker.resetUpCustomCompilerVersionForTest()
        }
    }

    protected fun createDir(name: String): File = tmpdir.resolve(name).apply { mkdirs() }
    protected fun createFile(name: String): File = tmpdir.resolve(name).apply { parentFile.mkdirs() }

    protected enum class WarningStatus { NO_WARNINGS, JS_OLD_LIBRARY_WARNING, JS_TOO_NEW_LIBRARY_WARNING, WASM_OLD_LIBRARY_WARNING, WASM_TOO_NEW_LIBRARY_WARNING }

    protected class TestVersion(val basicVersion: KotlinVersion, val postfix: String) : Comparable<TestVersion> {
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
    }
}

private fun zipDirectory(directory: File, zipFile: File) {
    KFile(directory.toPath()).zipDirAs(KFile(zipFile.toPath()))
}

