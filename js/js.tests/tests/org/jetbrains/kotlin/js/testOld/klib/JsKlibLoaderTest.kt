/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.js.testOld.utils.runJsCompiler
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.AbstractKlibLoaderTest
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import kotlin.io.path.absolutePathString
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString

@Suppress("JUnitTestCaseWithNoTests")
class JsKlibLoaderTest : AbstractKlibLoaderTest() {
    override val stdlib: String
        get() = StandardLibrariesPathProviderForKotlinProject.defaultJsStdlib().path

    override val ownPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.JS
        )

    override val alienPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.Wasm(),
            KlibPlatformChecker.Wasm(WasmTarget.JS.alias),
            KlibPlatformChecker.Wasm(WasmTarget.WASI.alias),
            KlibPlatformChecker.Native(),
            KlibPlatformChecker.Native(KonanTarget.IOS_ARM64.name),
            KlibPlatformChecker.NativeMetadata(KonanTarget.IOS_ARM64.name),
        )

    override fun compileKlib(
        parameters: CompilationParameters
    ) {
        runJsCompiler {
            if (parameters.asFile) {
                outputDir = parameters.klibLocation.parent.pathString
            } else {
                nopack = true
                outputDir = parameters.klibLocation.pathString
            }
            libraries = stdlib
            moduleName = parameters.sourceFile.nameWithoutExtension
            irModuleName = parameters.sourceFile.nameWithoutExtension
            customKlibAbiVersion = parameters.abiVersion.toString()
            freeArgs = listOf(parameters.sourceFile.absolutePathString())
            if (parameters.withCompanionBlocksAndExtensionsFeature) companionBlocksAndExtensions = true
        }
    }
}
