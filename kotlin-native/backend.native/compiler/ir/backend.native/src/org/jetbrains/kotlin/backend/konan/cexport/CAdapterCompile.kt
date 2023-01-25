/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.konan.exec.*
import org.jetbrains.kotlin.konan.target.*
import java.io.File

/**
 * Fourth phase of C export: compile runtime bindings to bitcode.
 */
fun produceCAdapterBitcode(clang: ClangArgs, cppFile: File, bitcodeFile: File) {
    val clangCommand = clang.clangCXX(
            "-std=c++17",
            cppFile.canonicalPath,
            "-emit-llvm", "-c",
            "-o", bitcodeFile.canonicalPath
    )
    Command(clangCommand).execute()
}
