/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * Copied from https://github.com/JetBrains/kotlin/blob/1.4.20/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/ReturnableBlockLowering.kt
 */

package com.bnorm.power.internal

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irComposite
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.transformStatement

// TODO Remove when inlining works correctly on Kotlin/JS and Kotlin/Native
class ReturnableBlockTransformer(val context: IrGeneratorContext, val containerSymbol: IrSymbol? = null) : IrElementTransformerVoidWithContext() {
  private var labelCnt = 0
  private val returnMap = mutableMapOf<IrReturnableBlockSymbol, (IrReturn) -> IrExpression>()

  override fun visitReturn(expression: IrReturn): IrExpression {
    expression.transformChildrenVoid()
    return returnMap[expression.returnTargetSymbol]?.invoke(expression) ?: expression
  }

  override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
    if (expression !is IrReturnableBlock) return super.visitContainerExpression(expression)

    val scopeSymbol = currentScope?.scope?.scopeOwnerSymbol ?: containerSymbol
    val builder = DeclarationIrBuilder(context, scopeSymbol!!)
    val variable by lazy {
      builder.scope.createTmpVariable(expression.type, "tmp\$ret\$${labelCnt++}", true)
    }

    val loop by lazy {
      IrDoWhileLoopImpl(
        expression.startOffset,
        expression.endOffset,
        context.irBuiltIns.unitType,
        expression.origin
      ).apply {
        label = "l\$ret\$${labelCnt++}"
        condition = builder.irBoolean(false)
      }
    }

    var hasReturned = false

    returnMap[expression.symbol] = { returnExpression ->
      hasReturned = true
      builder.irComposite(returnExpression) {
        +irSet(variable.symbol, returnExpression.value)
        +irBreak(loop)
      }
    }

    val newStatements = expression.statements.mapIndexed { i, s ->
      if (i == expression.statements.lastIndex && s is IrReturn && s.returnTargetSymbol == expression.symbol) {
        s.transformChildrenVoid()
        if (!hasReturned) s.value else {
          builder.irSet(variable.symbol, s.value)
        }
      } else {
        s.transformStatement(this)
      }
    }

    returnMap.remove(expression.symbol)

    if (!hasReturned) {
      return IrCompositeImpl(
        expression.startOffset,
        expression.endOffset,
        expression.type,
        expression.origin,
        newStatements
      )
    } else {
      loop.body = IrBlockImpl(
        expression.startOffset,
        expression.endOffset,
        context.irBuiltIns.unitType,
        expression.origin,
        newStatements
      )

      return builder.irComposite(expression, expression.origin) {
        +variable
        +loop
        +irGet(variable)
      }
    }
  }
}
