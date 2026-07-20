/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.NativeBackendContext
import org.jetbrains.kotlin.backend.konan.ir.annotations.exportedBridgeNonVirtualTargetMethod
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Rewrites the call to the target method inside a Swift Export forward bridge marked with
 * `@ExportedBridge(..., nonVirtualTargetMethod = "<method>")` so that every call to said method gets dispatched non-virtually,
 */
internal class ExportedBridgeNonVirtualLowering(val context: NativeBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        for (function in irFile.declarations.filterIsInstance<IrSimpleFunction>()) {
            val targetMethod = function.exportedBridgeNonVirtualTargetMethod ?: continue
            function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    expression.transformChildrenVoid()

                    val callee = expression.symbol.owner
                    if (callee.name.asString() != targetMethod) return expression
                    if (expression.superQualifierSymbol != null) return expression

                    val superClass = callee.parentClassOrNull ?: return expression
                    if (callee.resolveFakeOverride()?.modality == Modality.ABSTRACT) return expression

                    return IrCallImpl.fromSymbolOwner(
                            expression.startOffset,
                            expression.endOffset,
                            expression.type,
                            expression.symbol,
                            origin = expression.origin,
                            superQualifierSymbol = superClass.symbol,
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                    }
                }
            })
        }
    }
}
