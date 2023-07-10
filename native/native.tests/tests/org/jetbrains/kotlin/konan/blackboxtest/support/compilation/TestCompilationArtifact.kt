/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import java.io.File

internal sealed interface TestCompilationArtifact {
    val logFile: File

    data class KLIB(val klibFile: File) : TestCompilationArtifact {
        val path: String get() = klibFile.path
        override val logFile: File get() = klibFile.resolveSibling("${klibFile.name}.log")
    }

    data class KLIBStaticCache(val cacheDir: File, val klib: KLIB) : TestCompilationArtifact {
        override val logFile: File get() = cacheDir.resolve("${klib.klibFile.nameWithoutExtension}-cache.log")
    }

    data class Executable(val executableFile: File) : TestCompilationArtifact {
        val path: String get() = executableFile.path
        override val logFile: File get() = executableFile.resolveSibling("${executableFile.name}.log")
        val testDumpFile: File get() = executableFile.resolveSibling("${executableFile.name}.dump")
    }

    data class ObjCFramework(private val buildDir: File, val frameworkName: String) : TestCompilationArtifact {
        val frameworkDir: File get() = buildDir.resolve("$frameworkName.framework")
        override val logFile: File get() = frameworkDir.resolveSibling("${frameworkDir.name}.log")
        val headersDir: File get () = frameworkDir.resolve("Headers")
        val mainHeader: File get() = headersDir.resolve("$frameworkName.h")
    }
}
