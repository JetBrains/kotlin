/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.cli.common.arguments.ManualLanguageFeatureSetting
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.library.KLIB_PROPERTY_BUILTINS_PLATFORM
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.klib.compatibility.CompilerInvocationContext
import org.jetbrains.kotlin.test.klib.compatibility.LibrarySpecialCompatibilityChecksTest
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import java.io.File
import java.util.*

abstract class WebLibrarySpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {
    abstract val isWasm: Boolean

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

    override fun runCompiler(context: CompilerInvocationContext) {
        runJsCompiler(context.messageCollector, context.expectedExitCode) {
            this.freeArgs = listOf(context.sourceFile.absolutePath)
            this.libraries = (context.additionalLibraries + context.fakeLibraryPath).joinToString(File.pathSeparator)
            this.outputDir = context.outputDir.absolutePath
            this.moduleName = context.moduleName
            this.irProduceKlibFile = true
            this.irModuleName = context.moduleName
            this.wasm = isWasm
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
