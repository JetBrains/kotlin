/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningAllFunctionsKlibSecondStagePhase
import org.jetbrains.kotlin.backend.common.phaser.IrValidationAfterInliningPrivateFunctionsKlibPhase
import org.jetbrains.kotlin.backend.konan.NativeLoweringContext
import org.jetbrains.kotlin.ir.inline.isConsideredAsPrivateForInlining
import org.jetbrains.kotlin.ir.util.isTypeOfIntrinsic

internal class NativeIrValidationAfterInliningPrivateFunctionsKlibPhase(
        context: NativeLoweringContext
) : IrValidationAfterInliningPrivateFunctionsKlibPhase<NativeLoweringContext>(
        context = context,
        checkInlineFunctionCallSites = { inlineFunctionUseSite ->
            // Call sites of only non-private functions are allowed at this stage.
            !inlineFunctionUseSite.symbol.isConsideredAsPrivateForInlining()
        }
)

internal class NativeIrValidationAfterInliningAllFunctionsKlibSecondStagePhase(
        context: NativeLoweringContext
) : IrValidationAfterInliningAllFunctionsKlibSecondStagePhase<NativeLoweringContext>(
        context = context,
        checkInlineFunctionCallSites = check@{ inlineFunctionUseSite ->
            // No inline function call sites should remain at this stage.
            val inlineFunction = inlineFunctionUseSite.symbol.owner
            // it's fine to have typeOf<T>, it would be ignored by inliner and handled on the second stage of compilation
            if (inlineFunction.symbol.isTypeOfIntrinsic()) return@check true
            return@check inlineFunction.body == null
        }
)
