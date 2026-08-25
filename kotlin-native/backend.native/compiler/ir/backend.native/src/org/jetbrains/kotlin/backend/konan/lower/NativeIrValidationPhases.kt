/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningAllFunctionsKlibSecondStagePhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningPrivateFunctionsKlibPhase
import org.jetbrains.kotlin.backend.konan.NativeLoweringContext

// TODO: KT-88761. Remove these (the base class's constructor call sites are identical).
internal class NativeIrValidationAfterInliningPrivateFunctionsKlibPhase(
        context: NativeLoweringContext
) : IrValidationAfterInliningPrivateFunctionsKlibPhase<NativeLoweringContext>(
        context = context
)

internal class NativeIrValidationAfterInliningAllFunctionsKlibSecondStagePhase(
        context: NativeLoweringContext
) : IrValidationAfterInliningAllFunctionsKlibSecondStagePhase<NativeLoweringContext>(
        context = context
)
