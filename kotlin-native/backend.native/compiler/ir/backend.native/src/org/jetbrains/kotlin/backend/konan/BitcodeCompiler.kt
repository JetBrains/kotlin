/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import java.nio.file.Path

typealias ObjectFile = String

internal interface BitcodeCompiler<C> {
    fun makeObjectFile(bitcodeContainer: C, outputFilePath: Path)
}
