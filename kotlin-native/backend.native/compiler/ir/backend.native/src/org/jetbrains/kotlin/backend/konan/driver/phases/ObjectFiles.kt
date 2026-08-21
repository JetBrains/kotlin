/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.BitcodeCompiler
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import java.nio.file.Path

internal data class ObjectFilesPhaseInput(
        val bitcodeFile: Path,
        val objectFile: Path,
)

internal val ObjectFilesPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, ObjectFilesPhaseInput>(
        name = "ObjectFiles",
) { context, input ->
    BitcodeCompiler(context).makeObjectFile(input.bitcodeFile, input.objectFile)
}
