/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.backend.konan.serialization.KonanLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.cli.bc.K2Native
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.test.klib.compatibility.CompilerInvocationContext
import org.jetbrains.kotlin.test.klib.compatibility.LibrarySpecialCompatibilityChecksTest
import org.jetbrains.kotlin.test.klib.compatibility.StdlibSpecialCompatibilityChecksTest
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertNotNull
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

    override fun runCompiler(context: CompilerInvocationContext) {
        runNativeCompiler(context.messageCollector, context.expectedExitCode) {
            this.freeArgs = listOf(context.sourceFile.absolutePath)
            this.libraries = (context.additionalLibraries + context.fakeLibraryPath).toTypedArray()
            this.outputName = context.outputDir.resolve(context.moduleName).absolutePath
            this.moduleName = context.moduleName
            this.produce = "library"
            this.nostdlib = true
            this.kotlinHome = nativeHome.absolutePath
            if (context.exportKlibToOlderAbiVersion) {
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

    override val patchedLibraryPostfix: String = "native"

    @Test
    fun `getCompilerVersionFromKonanProperties returns non-null for real native stdlib from distribution`() {
        val result = KlibLoader { libraryPaths(nativeStdlibPath) }.load()
        result.reportLoadingProblemsIfAny { _, message -> fail(message) }
        val stdlibLibrary = result.librariesStdlibFirst.single()

        val version = KonanLibrarySpecialCompatibilityChecker.getCompilerVersionFromKonanProperties(stdlibLibrary)

        assertNotNull(version) {
            "getCompilerVersionFromKonanProperties() should return a non-null version for the real K/N stdlib at $nativeStdlibPath"
        }
    }
}

private fun runNativeCompiler(
    messageCollector: MessageCollectorImpl = MessageCollectorImpl(),
    expectedExitCode: ExitCode = ExitCode.OK,
    argsBuilder: K2NativeCompilerArguments.() -> Unit,
) {
    val args = K2NativeCompilerArguments().apply(argsBuilder)

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
}

@Suppress("JUnitTestCaseWithNoTests")
class StdLibNativeLibrarySpecialCompatibilityChecksTest : NativeLibrarySpecialCompatibilityChecksTest(),
    StdlibSpecialCompatibilityChecksTest {
    override val originalLibraryPath: String
        get() = patchedNativeStdlibWithoutJarManifest

    override val libraryDisplayName: String
        get() = "standard"
}