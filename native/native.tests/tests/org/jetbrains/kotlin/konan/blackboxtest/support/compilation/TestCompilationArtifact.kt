/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import java.io.File

internal sealed interface TestCompilationArtifact {
    val file: File
    val path: String get() = file.path

    data class KLIB(override val file: File) : TestCompilationArtifact
    data class KLIBStaticCache(override val file: File, val klib: KLIB) : TestCompilationArtifact
    data class Executable(override val file: File) : TestCompilationArtifact
}
