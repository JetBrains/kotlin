/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.AbstractKlibLoaderTest
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import java.io.File
import kotlin.test.fail

abstract class AbstractWasmKlibLoaderTest(private val target: WasmTarget) : AbstractKlibLoaderTest() {
    @Suppress("JUnitTestCaseWithNoTests")
    class WasmJsKlibLoaderTest : AbstractWasmKlibLoaderTest(WasmTarget.JS)

    @Suppress("JUnitTestCaseWithNoTests")
    class WasmWasiKlibLoaderTest : AbstractWasmKlibLoaderTest(WasmTarget.WASI)

    override val stdlib: String
        get() = WasmEnvironmentConfigurator.stdlibPath(target)

    override val ownPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.Wasm(),
            KlibPlatformChecker.Wasm(target.alias)
        )

    override val alienPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.JS,
            KlibPlatformChecker.Wasm(WasmTarget.entries.first { it != target }.alias),
            KlibPlatformChecker.Native(),
            KlibPlatformChecker.Native(KonanTarget.IOS_ARM64.name),
        )

    override fun compileKlib(
        asFile: Boolean,
        sourceFile: File,
        klibLocation: File,
        abiVersion: KotlinAbiVersion,
    ) {
        val args = K2JSCompilerArguments().apply {
            if (asFile) {
                irProduceKlibFile = true
                outputDir = klibLocation.parent
            } else {
                irProduceKlibDir = true
                outputDir = klibLocation.path
            }
            wasm = true
            wasmTarget = this@AbstractWasmKlibLoaderTest.target.alias
            libraries = stdlib
            moduleName = sourceFile.nameWithoutExtension
            irModuleName = sourceFile.nameWithoutExtension
            customKlibAbiVersion = abiVersion.toString()
            freeArgs = listOf(sourceFile.absolutePath)
        }

        val messageCollector = MessageCollectorImpl()

        val exitCode = K2JSCompiler().exec(messageCollector, Services.EMPTY, args)
        if (exitCode != ExitCode.OK) fail(
            buildString {
                appendLine("Compilation failed with exit code: $exitCode")
                appendLine("Command-line arguments: " + args.toArgumentStrings().joinToString(" "))
                appendLine("Compiler output:")
                appendLine(messageCollector.toString())
            }
        )
    }
}