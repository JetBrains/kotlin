/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.BitcodeCompiler
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ObjectFile

internal val ObjectFilesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, String, List<ObjectFile>>(
        name = "ObjectFiles",
        description = "Bitcode to object file",
        outputIfNotEnabled = { _, _, _, _ -> emptyList() }
) { context, bitcodeFileName ->
    BitcodeCompiler(context).makeObjectFiles(bitcodeFileName)
}