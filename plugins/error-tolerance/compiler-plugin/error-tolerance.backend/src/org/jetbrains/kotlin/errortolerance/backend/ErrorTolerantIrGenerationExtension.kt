/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.errortolerance.backend

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Replaces all error expressions left in the IR after the error-tolerant FIR2IR conversion with `throw java.lang.Error(message)`.
 *
 * Bodies of erroneous declarations are already replaced with error expressions by FIR2IR (according to the plan computed by
 * [org.jetbrains.kotlin.errortolerance.fir.ErroneousCodePlanBuilder]). This extension lowers them, as well as any other error
 * expression which might appear in the IR, so the JVM backend never sees them.
 *
 * Also, unresolved supertypes are removed, so classes with erroneous headers can still be loaded (with all their members stubbed).
 */
class ErrorTolerantIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val stringClass = pluginContext.irBuiltIns.stringClass
        val errorConstructor = pluginContext.finderForBuiltins().findConstructors(ERROR_CLASS_ID).singleOrNull { constructor ->
            constructor.owner.parameters.singleOrNull()?.type?.classOrNull == stringClass
        } ?: error("Constructor $ERROR_CLASS_ID(String) is not found")
        moduleFragment.transformChildren(ErrorExpressionLowering(pluginContext.irBuiltIns, errorConstructor), null)
    }

    private class ErrorExpressionLowering(
        private val irBuiltIns: IrBuiltIns,
        private val errorConstructor: IrConstructorSymbol,
    ) : IrElementTransformerVoid() {
        override fun visitClass(declaration: IrClass): IrStatement {
            if (declaration.superTypes.any { it is IrErrorType }) {
                declaration.superTypes = declaration.superTypes.filter { it !is IrErrorType }.ifEmpty { listOf(irBuiltIns.anyType) }
            }
            return super.visitClass(declaration)
        }

        override fun visitErrorExpression(expression: IrErrorExpression): IrExpression =
            buildThrow(expression, expression.description)

        override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrExpression {
            expression.transformChildren(this, null)
            return IrCompositeImpl(expression.startOffset, expression.endOffset, irBuiltIns.nothingType).apply {
                statements += listOfNotNull(expression.explicitReceiver)
                statements += expression.arguments
                statements += buildThrow(expression, expression.description)
            }
        }

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
            expression.transformChildren(this, null)
            if (expression.typeOperand !is IrErrorType) return expression
            return IrCompositeImpl(expression.startOffset, expression.endOffset, irBuiltIns.nothingType).apply {
                statements += expression.argument
                statements += buildThrow(expression, "Unresolved type: ${expression.typeOperand.render()}")
            }
        }

        private fun buildThrow(original: IrExpression, description: String): IrThrow {
            val message = when {
                description.startsWith(UNRESOLVED_COMPILATION_PROBLEM) -> description
                else -> "$UNRESOLVED_COMPILATION_PROBLEM:\n\t$description"
            }
            val errorType = errorConstructor.owner.constructedClass.defaultType
            val newError = IrConstructorCallImpl.fromSymbolOwner(original.startOffset, original.endOffset, errorType, errorConstructor).apply {
                arguments[0] = IrConstImpl.string(original.startOffset, original.endOffset, irBuiltIns.stringType, message)
            }
            return IrThrowImpl(original.startOffset, original.endOffset, irBuiltIns.nothingType, newError)
        }
    }

    companion object {
        private val ERROR_CLASS_ID = ClassId.topLevel(FqName("java.lang.Error"))
        private const val UNRESOLVED_COMPILATION_PROBLEM = "Unresolved compilation problem"
    }
}
