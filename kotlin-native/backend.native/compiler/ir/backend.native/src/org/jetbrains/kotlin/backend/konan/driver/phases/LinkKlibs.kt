/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.linkKlibs

internal val LinkKlibsPhase = createSimpleNamedCompilerPhase<PsiToIrContext, PsiToIrInput, PsiToIrOutput>(
        "LinkKlibs",
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ -> error("LinkKlibs phase cannot be disabled") }
) { context, input ->
    context.linkKlibs(input, useLinkerWhenProducingLibrary = false)
}
