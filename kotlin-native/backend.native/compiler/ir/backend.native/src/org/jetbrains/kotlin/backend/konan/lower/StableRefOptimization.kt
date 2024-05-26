/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name

internal class StableRefOptimization(
    val context: Context,
    val lifetimes: MutableMap<IrElement, Lifetime>,
) : FileLoweringPass, IrElementTransformer<IrDeclarationParent?> {
    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    private val interopPinned = context.ir.symbols.interopPinned
    private val createStablePointer = context.ir.symbols.createStablePointer
    private val createStackStablePointer = context.ir.symbols.createStackStablePointer
    private val stackStableRefConstructor = context.ir.symbols.interopStackStableRef.constructors.single()

    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent?): IrStatement {
        return super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: IrDeclarationParent?): IrExpression {
        expression.transformChildren(this, data)

        if (expression.symbol.owner.constructedClass.symbol != interopPinned || lifetimes[expression] != Lifetime.STACK)
            return expression

        val stablePointer = expression.getValueArgument(0) as? IrCall
        require(stablePointer != null && stablePointer.symbol == createStablePointer) {
            "The first argument is expected to be a call to ${createStablePointer.owner.name}: ${expression.render()}"
        }
        val irBuilder = context.createIrBuilder((data as IrSymbolOwner).symbol)
        return irBuilder.irBlock(expression) {
            val obj = stablePointer.getValueArgument(0)!!
            val temporary = IrVariableImpl(
                obj.startOffset, obj.endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, IrVariableSymbolImpl(),
                Name.identifier("obj"), obj.type, isVar = false, isConst = false, isLateinit = false
            ).apply {
                this.initializer = obj
                this.parent = data
            }
            val stackStablePointer = irBuilder.irCall(createStackStablePointer).apply { putValueArgument(0, irGet(temporary)) }
            expression.putValueArgument(0, stackStablePointer)
            val stackStableRef = irCallConstructor(stackStableRefConstructor, emptyList()).apply {
                putValueArgument(0, irGet(temporary))
            }
            lifetimes[stackStableRef] = Lifetime.STACK

            +temporary
            +stackStableRef
            +expression
        }
    }
}
