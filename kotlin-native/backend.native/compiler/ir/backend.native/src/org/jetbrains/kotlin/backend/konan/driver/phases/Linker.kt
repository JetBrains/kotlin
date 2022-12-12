/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.Linker
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ObjectFile

internal val LinkerPhase = createSimpleNamedCompilerPhase<NativeGenerationState, List<ObjectFile>>(
        name = "Linker",
        description = "Linker"
) { context, objectFiles ->
    // TODO: Explicit parameter
    Linker(context).link(objectFiles)
}