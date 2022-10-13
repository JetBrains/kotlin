/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.cli.klib.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact

internal fun TestCompilationArtifact.KLIB.getContents(): String {
    val output = StringBuilder()
    val lib = Library(klibFile.canonicalPath, null, "host")
    lib.contents(output, false)
    return output.toString()
}
