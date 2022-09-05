/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.coroutines.AbstractAddContinuationToFunctionCallsLowering
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.overrides

internal class NativeAddContinuationToFunctionCallsLowering(override val context: Context) : AbstractAddContinuationToFunctionCallsLowering() {
    /*
     * In complex cases suspend functions are converted to state-machine class with invokeSuspend method.
     * In that case continuation is an object itself
     * In simple cases, function is left as is, and receives continuation as its last parameter
     * We should handle both cases here
     */
    override fun IrSimpleFunction.getContinuationParameter() = when {
        overrides(context.ir.symbols.invokeSuspendFunction.owner) -> dispatchReceiverParameter!!
        else -> {
            valueParameters.lastOrNull().also {
                require(origin == IrDeclarationOrigin.LOWERED_SUSPEND_FUNCTION) { "Continuation parameter only exists in lowered suspend functions, but function origin is $origin" }
                require(it != null && it.origin == IrDeclarationOrigin.CONTINUATION) { "Continuation parameter is expected to be last one" }
            }!!
        }
    }
}