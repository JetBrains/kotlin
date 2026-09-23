/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import java.io.File

typealias ObjectFile = String

internal interface BitcodeCompiler<I> {
    fun makeObjectFile(bitcodeContainer: I, outputObjectFile: File)
}
