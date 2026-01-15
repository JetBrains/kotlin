/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_LIBRARY_VERSION
import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker.Companion.KLIB_JAR_MANIFEST_FILE
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.konan.file.createTempDir
import org.jetbrains.kotlin.konan.file.unzipTo
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import java.io.File
import java.util.*
import java.util.jar.Manifest
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.konan.file.File as KlibFile

abstract class LibrarySpecialCompatibilityChecksTest : TestCaseWithTmpdir() {
    abstract val isWasm: Boolean

    fun testSameBasicCompilerVersion() {
        for (versionsWithSameBasicVersion in SORTED_TEST_COMPILER_VERSION_GROUPS) {
            for (libraryVersion in versionsWithSameBasicVersion) {
                for (compilerVersion in versionsWithSameBasicVersion) {
                    compileDummyLibrary(
                        libraryVersion = libraryVersion,
                        compilerVersion = compilerVersion,
                        expectedWarningStatus = WarningStatus.NO_WARNINGS
                    )
                }
            }
        }
    }

    fun testNewerCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            compileDummyLibrary(
                libraryVersion = currentVersion,
                compilerVersion = nextVersion,
                expectedWarningStatus = WarningStatus.OLD_LIBRARY_WARNING
            )
        }
    }

    fun testOlderCompilerVersion() {
        testCurrentAndNextBasicVersions { currentVersion, nextVersion ->
            val sameLanguageVersion = haveSameLanguageVersion(currentVersion, nextVersion)
            compileDummyLibrary(
                libraryVersion = nextVersion,
                compilerVersion = currentVersion,
                expectedWarningStatus = if (sameLanguageVersion) WarningStatus.NO_WARNINGS else WarningStatus.TOO_NEW_LIBRARY_WARNING
            )
        }
    }

    private inline fun testCurrentAndNextBasicVersions(block: (currentVersion: TestVersion, nextVersion: TestVersion) -> Unit) {
        for (i in 0..SORTED_TEST_COMPILER_VERSION_GROUPS.size - 2) {
            val versionsWithSameBasicVersion = SORTED_TEST_COMPILER_VERSION_GROUPS[i]
            val versionsWithNextSameBasicVersion = SORTED_TEST_COMPILER_VERSION_GROUPS[i + 1]

            for (currentVersion in versionsWithSameBasicVersion) {
                for (nextVersion in versionsWithNextSameBasicVersion) {
                    block(currentVersion, nextVersion)
                }
            }
        }
    }

    fun testEitherVersionIsMissing() {
        listOf(
            TestVersion(2, 0, 0) to null,
            null to TestVersion(2, 0, 0),
        ).forEach { (libraryVersion, compilerVersion) ->
            compileDummyLibrary(
                libraryVersion = libraryVersion,
                compilerVersion = compilerVersion,
                expectedWarningStatus = WarningStatus.NO_WARNINGS
            )
        }
    }

    /**
     * Since the ABI version is bumped after the language version, it may happen that after bumping the language version
     * [KotlinAbiVersion.CURRENT] != [LanguageVersion.LATEST_STABLE]. This can cause issues in library compatibility tests: for example,
     * when exporting a klib to the previous ABI version, we may use a 2.X compiler while the previous ABI version is 2.(X − 2).
     * Since this is only a temporary situation (the ABI version is usually bumped shortly after the language version),
     * we simply ignore these tests when this happens.
     */
    override fun shouldRunTest(): Boolean =
        LanguageVersion.LATEST_STABLE.major == KotlinAbiVersion.CURRENT.major && LanguageVersion.LATEST_STABLE.minor == KotlinAbiVersion.CURRENT.minor

    internal fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean = false,
    ) {
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = false, expectedWarningStatus, exportKlibToOlderAbiVersion)
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = true, expectedWarningStatus, exportKlibToOlderAbiVersion)
    }

    protected abstract val libraryDisplayName: String

    private fun MessageCollectorImpl.hasOldLibraryError(
        specificVersions: Pair<TestVersion, TestVersion>? = null,
    ): Boolean {
        val platformDisplayName = if (isWasm) "Kotlin/Wasm" else "Kotlin/JS"

        val stdlibMessagePart = "$platformDisplayName $libraryDisplayName library has an older version" +
                specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "than the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    private fun MessageCollectorImpl.hasTooNewLibraryError(
        libraryVersion: TestVersion? = null,
        abiCompatibilityLevel: KlibAbiCompatibilityLevel? = null,
    ): Boolean {
        val platformDisplayName = if (isWasm) "Kotlin/Wasm" else "Kotlin/JS"

        val stdlibMessagePart = "The $platformDisplayName $libraryDisplayName library has the ABI version" +
                libraryVersion?.let { " (${it.basicVersion.major}.${it.basicVersion.minor}.0)" }.orEmpty()
        val compilerMessagePart = "that is not compatible with the compiler's current ABI compatibility level ($abiCompatibilityLevel)"

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }

    }

    private fun MessageCollectorImpl.checkMessage(
        expectedWarningStatus: WarningStatus,
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        abiCompatibilityLevel: KlibAbiCompatibilityLevel?,
    ) {
        val success = when (expectedWarningStatus) {
            WarningStatus.NO_WARNINGS -> !hasOldLibraryError() && !hasTooNewLibraryError()
            WarningStatus.OLD_LIBRARY_WARNING -> hasOldLibraryError(libraryVersion!! to compilerVersion!!)
            WarningStatus.TOO_NEW_LIBRARY_WARNING -> hasTooNewLibraryError(libraryVersion!!, abiCompatibilityLevel)
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
        isZipped: Boolean,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean,
    ) {
        val sourcesDir = createDir("sources")
        val outputDir = createDir("build")

        val sourceFile = sourcesDir.resolve("file.kt").apply { writeText("fun foo() = 42\n") }
        val moduleName = getTestName(true)

        val messageCollector = MessageCollectorImpl()

        withCustomCompilerVersion(compilerVersion) {
            val fakeLibrary = if (isZipped)
                createFakeZippedLibraryWithSpecificVersion(libraryVersion)
            else
                createFakeUnzippedLibraryWithSpecificVersion(libraryVersion)

            val expectedExitCode = if (expectedWarningStatus == WarningStatus.NO_WARNINGS) ExitCode.OK else ExitCode.COMPILATION_ERROR
            runJsCompiler(messageCollector, expectedExitCode) {
                this.freeArgs = listOf(sourceFile.absolutePath)
                this.libraries = (additionalLibraries() + fakeLibrary.absolutePath).joinToString(File.pathSeparator)
                this.outputDir = outputDir.absolutePath
                this.moduleName = moduleName
                this.irProduceKlibFile = true
                this.irModuleName = moduleName
                this.wasm = isWasm
                if (exportKlibToOlderAbiVersion) {
                    this.languageVersion = "${LanguageVersion.LATEST_STABLE.major}.${LanguageVersion.LATEST_STABLE.minor - 1}"
                    this.internalArguments = listOf(
                        ManualLanguageFeatureSetting(
                            LanguageFeature.ExportKlibToOlderAbiVersion,
                            LanguageFeature.State.ENABLED,
                            "-XXLanguage:+ExportKlibToOlderAbiVersion"
                        )
                    )
                }
            }
        }

        val klibAbiCompatibilityLevel =
            if (exportKlibToOlderAbiVersion) KlibAbiCompatibilityLevel.LATEST_STABLE.previous()!! else KlibAbiCompatibilityLevel.LATEST_STABLE
        messageCollector.checkMessage(expectedWarningStatus, libraryVersion, compilerVersion, klibAbiCompatibilityLevel)
    }

    private fun haveSameLanguageVersion(a: TestVersion, b: TestVersion): Boolean =
        a.basicVersion.major == b.basicVersion.major && a.basicVersion.minor == b.basicVersion.minor

    protected abstract val originalLibraryPath: String
    protected open fun additionalLibraries(): List<String> = listOf()

    private fun createFakeUnzippedLibraryWithSpecificVersion(version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedLibraryDir = createDir("dependencies/fakeLib-${rawVersion ?: "unknown"}-${if (isWasm) "wasm" else "js"}")
        val manifestFile = patchedLibraryDir.resolve("default").resolve("manifest")
        if (manifestFile.exists()) return patchedLibraryDir

        val originalLibraryFile = File(originalLibraryPath)

        if (originalLibraryFile.isDirectory) {
            originalLibraryFile.copyRecursively(patchedLibraryDir)
        } else {
            KlibFile(originalLibraryPath).unzipTo(KlibFile(patchedLibraryDir.absolutePath))
            // Zipped version of KLIB always has a manifest file, so we delete it inside the patchedLibraryDir
            // just after unzipping, to replace with the test one
        }

        if (version != null) {
            val properties = manifestFile.inputStream().use { Properties().apply { load(it) } }
            properties[KLIB_PROPERTY_ABI_VERSION] = KotlinAbiVersion(version.basicVersion.major, version.basicVersion.minor, 0).toString()
            manifestFile.outputStream().use { properties.store(it, null) }
        }

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

    private fun createFakeZippedLibraryWithSpecificVersion(version: TestVersion?): File {
        val rawVersion = version?.toString()

        val patchedLibraryFile = createFile("dependencies/fakeLib-${rawVersion ?: "unknown"}-${if (isWasm) "wasm" else "js"}.klib")
        if (patchedLibraryFile.exists()) return patchedLibraryFile

        val unzippedLibraryDir = createFakeUnzippedLibraryWithSpecificVersion(version)
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

    internal enum class WarningStatus { NO_WARNINGS, OLD_LIBRARY_WARNING, TOO_NEW_LIBRARY_WARNING }

    internal class TestVersion(val basicVersion: KotlinVersion, val postfix: String) : Comparable<TestVersion> {
        constructor(major: Int, minor: Int, patch: Int, postfix: String = "") : this(KotlinVersion(major, minor, patch), postfix)

        override fun compareTo(other: TestVersion) = basicVersion.compareTo(other.basicVersion)
        override fun equals(other: Any?) = (other as? TestVersion)?.basicVersion == basicVersion
        override fun hashCode() = basicVersion.hashCode()
        override fun toString() = basicVersion.toString() + postfix
    }

    companion object {
        private val currentKotlinVersion = KotlinVersion.CURRENT

        private val VERSIONS = listOf(
            0 to "",
            0 to "-dev-1234",
            0 to "-dev-4321",
            0 to "-Beta1",
            0 to "-Beta2",
            20 to "",
            20 to "-Beta1",
            20 to "-Beta2",
            255 to "-SNAPSHOT",
        )

        internal val SORTED_TEST_COMPILER_VERSION_GROUPS: List<Collection<TestVersion>> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor, patch, postfix) }
                .groupByTo(TreeMap()) { it.basicVersion }.values.toList()

        internal val SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS: List<TestVersion> =
            VERSIONS.map { (patch, postfix) -> TestVersion(currentKotlinVersion.major, currentKotlinVersion.minor - 1, patch, postfix) }

        val patchedJsStdlibWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            createPatchedStdlib(JsEnvironmentConfigurator.stdlibPath)
        }

        val patchedWasmStdlibWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            createPatchedStdlib(WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS))
        }

        val patchedJsTestWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            createPatchedStdlib(JsEnvironmentConfigurator.kotlinTestPath)
        }

        val patchedWasmTestWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            createPatchedStdlib(WasmEnvironmentConfigurator.kotlinTestPath(WasmTarget.JS))
        }

        private fun createPatchedStdlib(stdlibPath: String): String {
            val src = File(stdlibPath)
            val stdlibName = (if (src.isDirectory) src.name else src.nameWithoutExtension)
            val patchedStdlibDir = File(createTempDir(stdlibName).absolutePath)
            if (src.isDirectory) {
                src.copyRecursively(patchedStdlibDir, overwrite = true)
            } else {
                KlibFile(stdlibPath).unzipTo(KlibFile(patchedStdlibDir.absolutePath))
            }
            patchedStdlibDir.resolve(KLIB_JAR_MANIFEST_FILE).delete()
            return patchedStdlibDir.absolutePath
        }
    }
}

private fun zipDirectory(directory: File, zipFile: File) {
    KFile(directory.toPath()).zipDirAs(KFile(zipFile.toPath()))
}

