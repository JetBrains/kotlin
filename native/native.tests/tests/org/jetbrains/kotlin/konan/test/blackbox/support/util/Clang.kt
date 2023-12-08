/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import java.io.File

/**
 * Specifies which Clang should be used for compilation.
 */
internal enum class ClangDistribution {
    /**
     * Use Clang from target's toolchain.
     * Note that not all targets do have a toolchain with Clang.
     * For example, Apple targets do (from Xcode), and GCC-based Linux targets do not.
     */
    Toolchain,

    /**
     * Use Clang from Kotlin/Native LLVM distribution.
     */
    LlvmDistribution
}

// FIXME: absoluteTargetToolchain might not work correctly with KONAN_USE_INTERNAL_SERVER because
// :kotlin-native:dependencies:update is not a dependency of :native:native.tests:test where this test runs
internal fun AbstractNativeSimpleTest.compileWithClang(
    clangDistribution: ClangDistribution = ClangDistribution.LlvmDistribution,
    sourceFiles: List<File>,
    outputFile: File,
    includeDirectories: List<File> = emptyList(),
    frameworkDirectories: List<File> = emptyList(),
    libraryDirectories: List<File> = emptyList(),
    libraries: List<String> = emptyList(),
    additionalLinkerFlags: List<String> = emptyList(),
) {
    val clang = when (clangDistribution) {
        ClangDistribution.Toolchain -> "${testRunSettings.configurables.absoluteTargetToolchain}/bin/clang"
        ClangDistribution.LlvmDistribution -> "${testRunSettings.configurables.absoluteLlvmHome}/bin/clang"
    }
    val process = ProcessBuilder(
        clang,
        *sourceFiles.map { it.absolutePath }.toTypedArray(),
        *includeDirectories.flatMap { listOf("-I", it.absolutePath) }.toTypedArray(),
        "-isysroot", testRunSettings.configurables.absoluteTargetSysRoot,
        "-target", testRunSettings.configurables.targetTriple.toString(),
        "-g", "-fmodules",
        *frameworkDirectories.flatMap { listOf("-F", it.absolutePath) }.toTypedArray(),
        *libraryDirectories.flatMap { listOf("-L", it.absolutePath) }.toTypedArray(),
        *libraries.map { "-l$it" }.toTypedArray(),
        *additionalLinkerFlags.toTypedArray(),
        "-o", outputFile.absolutePath
    ).redirectErrorStream(true).start()
    val clangOutput = process.inputStream.readBytes()

    check(
        process.waitFor() == 0
    ) { clangOutput.decodeToString() }
}