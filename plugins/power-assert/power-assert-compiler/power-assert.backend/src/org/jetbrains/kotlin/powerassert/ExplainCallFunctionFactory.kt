/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

class ExplainCallFunctionFactory(
    private val module: IrModuleFragment,
    private val context: IrPluginContext,
    private val builtIns: PowerAssertBuiltIns,
) {
    private var IrSimpleFunction.explainedDispatchFunctionSymbol: IrSimpleFunctionSymbol? by irAttribute(followAttributeOwner = false)

    fun find(function: IrSimpleFunction): IrSimpleFunctionSymbol? {
        function.explainedDispatchFunctionSymbol?.let { return it }

        // TODO this never works because... the generated function has no metadata? need to create an FIR function as well?
        val callableId = function.callableId.copy(Name.identifier(function.name.identifier + "\$explained"))
        context.referenceFunctions(callableId).singleOrNull { it.isSyntheticFor(function) }?.let { return it }

        // TODO is this the best way to handle compilation unit checks?
//        if (function.fileOrNull !in module.files) return null
        return generate(function).symbol
    }

    fun generate(function: IrSimpleFunction): IrSimpleFunction {
        val newFunction = function.deepCopyWithSymbols(function.parent).apply {
            origin = FUNCTION_FOR_EXPLAIN_CALL
            name = Name.identifier(function.name.identifier + "\$explained")
            annotations = annotations.filter { !it.isAnnotationWithEqualFqName(builtIns.explainCallType.classFqName!!) } +
                    createJvmSyntheticAnnotation()
            addValueParameter {
                name = Name.identifier("\$explanation") // TODO what if there's another property with this name?
                type = builtIns.callExplanationType
            }
        }

        // Transform the generated function to use the `$explanation` parameter instead of Explain.explanation.
        val diagramParameter = newFunction.valueParameters.last()
        newFunction.transformChildrenVoid(DiagramDispatchTransformer(diagramParameter, context))
        function.explainedDispatchFunctionSymbol = newFunction.symbol

        // Transform the original function to use `null` instead of Explain.explanation.
        // This keeps the code from throwing an error when Explain.explanation.
        // This in turn helps make sure the compiler-plugin is applied to functions which use `@Explain`.
        // TODO should this be in the transformer and not here?
        function.transformChildrenVoid(DiagramDispatchTransformer(explanation = null, context))

        return newFunction
    }

    private fun createJvmSyntheticAnnotation(): IrConstructorCallImpl {
        val symbol = context.referenceConstructors(JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID).single()
        return IrConstructorCallImpl.fromSymbolOwner(
            startOffset = SYNTHETIC_OFFSET,
            endOffset = SYNTHETIC_OFFSET,
            type = symbol.owner.parentAsClass.defaultType,
            constructorSymbol = symbol,
        )
    }

    private class DiagramDispatchTransformer(
        private val explanation: IrValueParameter?,
        private val context: IrPluginContext,
    ) : IrElementTransformerVoidWithContext() {
        override fun visitExpression(expression: IrExpression): IrExpression {
            return when {
                isExplanation(expression) -> when (explanation) {
                    null -> IrConstImpl.constNull(expression.startOffset, expression.endOffset, context.irBuiltIns.anyType.makeNullable())
                    else -> IrGetValueImpl(expression.startOffset, expression.endOffset, explanation.type, explanation.symbol)
                }
                else -> super.visitExpression(expression)
            }
        }

        private fun isExplanation(expression: IrExpression): Boolean =
            (expression as? IrCall)?.symbol?.owner?.kotlinFqName == PowerAssertGetDiagram
    }

    private fun IrSimpleFunctionSymbol.isSyntheticFor(function: IrSimpleFunction): Boolean {
        // TODO need to consider type parameters and how they differ.

        val owner = owner
        if (function.dispatchReceiverParameter?.type != owner.dispatchReceiverParameter?.type) return false
        if (function.extensionReceiverParameter?.type != owner.extensionReceiverParameter?.type) return false

        if (function.valueParameters.size != owner.valueParameters.size - 1) return false
        for (index in function.valueParameters.indices) {
            if (function.valueParameters[index].type != owner.valueParameters[index].type) return false
        }

        return owner.valueParameters.last().type == builtIns.callExplanationType
    }
}
