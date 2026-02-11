/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.config.KlibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.klib.compatibility.LibrarySpecialCompatibilityChecksTest
import org.jetbrains.kotlin.klib.compatibility.TestVersion
import org.jetbrains.kotlin.klib.compatibility.WarningStatus
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import java.io.File
import java.util.*

abstract class WebLibrarySpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {
    abstract val isWasm: Boolean

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
        createPatchedLibrary(JsEnvironmentConfigurator.stdlibPath)
    }

    val patchedWasmStdlibWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createPatchedLibrary(WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS))
    }

    val patchedJsTestWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createPatchedLibrary(JsEnvironmentConfigurator.kotlinTestPath)
    }

    val patchedWasmTestWithoutJarManifest by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createPatchedLibrary(WasmEnvironmentConfigurator.kotlinTestPath(WasmTarget.JS))
    }

    override val platformDisplayName get() = if (isWasm) "Kotlin/Wasm" else "Kotlin/JS"

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

    override val patchedLibraryPostfix: String
        get() = if (isWasm) "wasm" else "js"

    override fun additionalPatchedLibraryProperties(manifestFile: File) {
        if (isWasm) {
            val properties = manifestFile.inputStream().use { Properties().apply { load(it) } }
            properties[KLIB_PROPERTY_BUILTINS_PLATFORM] = BuiltInsPlatform.WASM.name
            manifestFile.outputStream().use { properties.store(it, null) }
        }
    }
}
