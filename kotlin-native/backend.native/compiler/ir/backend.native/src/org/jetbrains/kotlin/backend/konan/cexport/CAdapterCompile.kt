/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.backend.konan.util.absoluteNormalizedPathString
import org.jetbrains.kotlin.konan.exec.*
import org.jetbrains.kotlin.konan.target.*
import java.nio.file.Path

/**
 * Fourth phase of C export: compile runtime bindings to bitcode.
 */
fun produceCAdapterBitcode(clang: ClangArgs, cppFile: Path, bitcodeFile: Path) {
    val clangCommand = clang.clangCXX(
            "-std=c++17",
            cppFile.absoluteNormalizedPathString(),
            "-emit-llvm", "-c",
            "-o", bitcodeFile.absoluteNormalizedPathString()
    )
    Command(clangCommand).execute()
}
