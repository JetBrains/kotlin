/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.isObjCForwardDeclaration
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// TODO: Similar to IrType.erasedUpperBound from jvm.ir
internal fun IrType.erasure(): IrType {
    if (this !is IrSimpleType) return this

    val upperBound = when (val classifier = classifier) {
        is IrClassSymbol -> classifier.defaultType
        is IrTypeParameterSymbol -> {
            // Pick the (necessarily unique) non-interface upper bound if it exists
            classifier.owner.superTypes.firstOrNull {
                it.classOrNull?.owner?.isInterface == false
            } ?:
            // Otherwise, choose either the first IrClass supertype or recurse.
            // In the first case, all supertypes are interface types and the choice was arbitrary.
            // In the second case, there is only a single supertype.
            classifier.owner.superTypes.first().erasure()
        }
        is IrScriptSymbol -> classifier.unexpectedSymbolKind<IrClassifierSymbol>()
    }

    return upperBound.mergeNullability(this)
}

internal val IrType.erasedUpperBound get() = this.erasure().getClass() ?: error(this.render())

internal class TypeOperatorLowering(val context: CommonBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    private fun effectiveCheckType(type: IrType) : IrType {
        val erasedType = type.erasure()
        return if (erasedType.classOrNull?.owner?.isObjCForwardDeclaration() == true) {
            context.irBuiltIns.anyType.mergeNullability(erasedType)
        } else {
            erasedType
        }
    }

    private fun lowerCast(expression: IrTypeOperatorCall): IrExpression {
        builder.at(expression)
        val typeOperand = effectiveCheckType(expression.typeOperand)
        return if (typeOperand == expression.typeOperand) {
            expression
        } else {
            builder.irAs(expression.argument, typeOperand)
        }
    }

    private fun lowerSafeCast(expression: IrTypeOperatorCall): IrExpression {
        val typeOperand = effectiveCheckType(expression.typeOperand)

        return builder.irBlock(expression) {
            +irLetS(expression.argument) { variable ->
                irIfThenElse(expression.type,
                        condition = irIs(irGet(variable.owner), typeOperand),
                        thenPart = irImplicitCast(irGet(variable.owner), typeOperand),
                        elsePart = irNull())
            }
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        return when (expression.operator) {
            IrTypeOperator.SAFE_CAST -> lowerSafeCast(expression)
            IrTypeOperator.CAST -> lowerCast(expression)
            else -> expression
        }
    }
}
