/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths
import java.io.File
import java.nio.file.Files

internal fun createTempSourceRoot(): File = Files.createTempDirectory("kdm-analyzer-test").toFile()

/**
 * The built stdlib artifact for [platform], read from the `kotlin.*` system properties that the
 * `projectTests { with*Runtime() }` helpers in this module's build script inject into the test JVM -
 * the same standard mechanism the compiler and Analysis API test infrastructure use (see [TestCompilePaths]
 * for the full property list).
 *
 * Kotlin/Native has its own property, wired only when Kotlin/Native is enabled - see [nativeStdlibKlib].
 */
internal fun stdlibFor(platform: KdAnalyzerPlatform): File = when (platform) {
    KdAnalyzerPlatform.JVM -> ForTestCompileRuntime.runtimeJarForTests()
    KdAnalyzerPlatform.COMMON -> ForTestCompileRuntime.stdlibCommonForTests()
    KdAnalyzerPlatform.JS -> ForTestCompileRuntime.stdlibJsForTests()
    KdAnalyzerPlatform.WASM_JS -> ForTestCompileRuntime.getFileFromProperty(TestCompilePaths.KOTLIN_WASM_JS_STDLIB_KLIB_PATH)
    KdAnalyzerPlatform.WASM_WASI -> ForTestCompileRuntime.getFileFromProperty(TestCompilePaths.KOTLIN_WASM_WASI_STDLIB_KLIB_PATH)
    KdAnalyzerPlatform.NATIVE -> nativeStdlibKlib
}

/**
 * The Kotlin/Native stdlib klib built from this repository (`:kotlin-native:runtime:nativeStdlib`).
 *
 * The build script passes it via the `kotlin.native.stdlib.klib.path` system property only when
 * `kotlin.native.enabled=true`, since `:kotlin-native:*` projects don't exist otherwise.
 * Without it, tests that need this klib fail.
 */
internal val nativeStdlibKlib: File
    get() = File(
        System.getProperty(NATIVE_STDLIB_KLIB_PATH_PROPERTY)
            ?: error(
                "Missing system property '$NATIVE_STDLIB_KLIB_PATH_PROPERTY': this test requires the Kotlin/Native stdlib. " +
                        "Pass `-Pkotlin.native.enabled=true` to Gradle (or set it in local.properties)."
            )
    )

private const val NATIVE_STDLIB_KLIB_PATH_PROPERTY = "kotlin.native.stdlib.klib.path"
