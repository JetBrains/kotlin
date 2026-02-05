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
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.*
import java.util.jar.Manifest
import kotlin.test.fail
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.konan.file.File as KlibFile

abstract class WebLibrarySpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {
    abstract val isWasm: Boolean

    private lateinit var testName: String

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        testName = testInfo.testMethod.get().name
    }

    override fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean,
    ) {
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = false, expectedWarningStatus, exportKlibToOlderAbiVersion)
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = true, expectedWarningStatus, exportKlibToOlderAbiVersion)
    }

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
        val moduleName = testName

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
}

private fun zipDirectory(directory: File, zipFile: File) {
    KFile(directory.toPath()).zipDirAs(KFile(zipFile.toPath()))
}
