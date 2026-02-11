/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.klib.compatibility.LibrarySpecialCompatibilityChecksTest
import org.jetbrains.kotlin.klib.compatibility.StdlibSpecialCompatibilityChecksTest
import org.jetbrains.kotlin.klib.compatibility.TestVersion
import org.jetbrains.kotlin.klib.compatibility.WarningStatus
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import java.io.File
import kotlin.test.fail

abstract class NativeLibrarySpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {

    private val nativeHome: File
        get() = currentCustomNativeCompilerSettings.nativeHome

    val patchedNativeStdlibWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createPatchedLibrary(nativeStdlibPath)
    }

    private val nativeStdlibPath: String
        get() = nativeHome.resolve(KONAN_DISTRIBUTION_KLIB_DIR)
            .resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
            .resolve(KONAN_STDLIB_NAME)
            .absolutePath

    override val platformDisplayName = "Kotlin/Native"

    override fun compileDummyLibrary(
        libraryVersion: TestVersion?,
        compilerVersion: TestVersion?,
        expectedWarningStatus: WarningStatus,
        exportKlibToOlderAbiVersion: Boolean,
    ) {
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = false, expectedWarningStatus, exportKlibToOlderAbiVersion)
        compileDummyLibrary(libraryVersion, compilerVersion, isZipped = true, expectedWarningStatus, exportKlibToOlderAbiVersion)
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
            runNativeCompiler(nativeHome.absolutePath, messageCollector, expectedExitCode) {
                this.freeArgs = listOf(sourceFile.absolutePath)
                this.libraries = (additionalLibraries() + fakeLibrary.absolutePath).toTypedArray()
                this.outputName = outputDir.resolve(moduleName).absolutePath
                this.moduleName = moduleName
                this.produce = "library"
                this.nostdlib = true
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

    override val patchedLibraryPostfix: String = "native"

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

private fun runNativeCompiler(
    nativeHome: String,
    messageCollector: MessageCollectorImpl = MessageCollectorImpl(),
    expectedExitCode: ExitCode = ExitCode.OK,
    argsBuilder: K2NativeCompilerArguments.() -> Unit,
) {
    val args = K2NativeCompilerArguments().apply(argsBuilder)

    val oldKonanHome = System.getProperty("konan.home")
    try {
        System.setProperty("konan.home", nativeHome)
        val exitCode = K2Native().exec(messageCollector, Services.EMPTY, args)
        if (exitCode != expectedExitCode) fail(
            buildString {
                appendLine("Unexpected compiler exit code:")
                appendLine("  Expected: $expectedExitCode")
                appendLine("  Actual:   $exitCode")
                appendLine("Command-line arguments: " + args.toArgumentStrings().joinToString(" "))
                appendLine("Compiler output:")
                appendLine(messageCollector.toString())
            }
        )
    } finally {
        if (oldKonanHome != null) {
            System.setProperty("konan.home", oldKonanHome)
        } else {
            System.clearProperty("konan.home")
        }
    }
}

@Suppress("JUnitTestCaseWithNoTests")
class StdLibNativeLibrarySpecialCompatibilityChecksTest : NativeLibrarySpecialCompatibilityChecksTest(),
    StdlibSpecialCompatibilityChecksTest {
    override val originalLibraryPath: String
        get() = patchedNativeStdlibWithoutJarManifest

    override val libraryDisplayName: String
        get() = "standard"
}