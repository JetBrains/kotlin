/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.compilerRunner.toArgumentStrings
import org.jetbrains.kotlin.klib.AbstractKlibLoaderTest
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.callCompilerWithoutOutputInterceptor
import org.jetbrains.kotlin.konan.test.blackbox.support.copyNativeHomeProperty
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.test.fail

@Tag("klib")
abstract class AbstractNativeKlibLoadingTest(private val target: KonanTarget) : AbstractKlibLoaderTest() {
    @Suppress("JUnitTestCaseWithNoTests")
    class NativeMacosArm64KlibLoadingTest : AbstractNativeKlibLoadingTest(KonanTarget.MACOS_ARM64)

    @Suppress("JUnitTestCaseWithNoTests")
    class NativeLinuxArm64KlibLoadingTest : AbstractNativeKlibLoadingTest(KonanTarget.LINUX_ARM64)

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            copyNativeHomeProperty()
        }
    }

    override val stdlib: String
        get() = NativeTestSupport.computeNativeHome().stdlibFile.path

    override val ownPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.Native(),
            KlibPlatformChecker.Native(target.name),
        )

    override val alienPlatformCheckers: List<KlibPlatformChecker>
        get() = listOf(
            KlibPlatformChecker.JS,
            KlibPlatformChecker.Wasm(),
            KlibPlatformChecker.Wasm(WasmTarget.JS.alias),
            KlibPlatformChecker.Wasm(WasmTarget.WASI.alias),
            KlibPlatformChecker.Native(KonanTarget.predefinedTargets.values.first { it != target }.name),
        )

    override fun compileKlib(
        asFile: Boolean,
        sourceFile: File,
        klibLocation: File,
        abiVersion: KotlinAbiVersion,
    ) {
        runNativeCompiler {
            produce = "library"
            target = this@AbstractNativeKlibLoadingTest.target.name
            nopack = !asFile
            outputName = klibLocation.path
            libraries = arrayOf(stdlib)
            customKlibAbiVersion = abiVersion.toString()
            freeArgs = listOf(sourceFile.absolutePath)
        }
    }
}

internal fun runNativeCompiler(argsBuilder: K2NativeCompilerArguments.() -> Unit) {
    val args = K2NativeCompilerArguments().apply(argsBuilder)
    val argsArray = args.toArgumentStrings().toTypedArray()

    val result = callCompilerWithoutOutputInterceptor(argsArray)
    if (result.exitCode != ExitCode.OK) fail(
        buildString {
            appendLine("Compilation failed with exit code: ${result.exitCode}")
            appendLine("Command-line arguments: " + argsArray.joinToString(" "))
            appendLine("Compiler output:")
            appendLine(result.toolOutput)
        }
    )
}
