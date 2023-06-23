/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.plugin.types.ComposableNames.FULL_COMPOSABLE_NAME_PREFIX
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeBase
import org.jetbrains.kotlin.ir.types.impl.IrTypeProjectionImpl
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class ComposableFunctionsTransformer(val pluginContext: IrPluginContext) : IrElementVisitorVoid {
    companion object {
        private val INVOKE = Name.identifier("invoke")
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        declaration.apply {
            type = type.update()
        }
        visitElement(declaration)
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.apply {
            returnType = returnType.update()
        }
        visitElement(declaration)
    }

    override fun visitCall(expression: IrCall) {
        updateReferenceInCallIfNeeded(expression)
        visitElement(expression)
    }

    override fun visitVariable(declaration: IrVariable) {
        declaration.apply {
            type = type.update()
        }
        visitElement(declaration)
    }

    override fun visitExpression(expression: IrExpression) {
        expression.apply {
            type = type.update()
        }
        visitElement(expression)
    }

    private fun updateReferenceInCallIfNeeded(call: IrCall) {
        val function = call.symbol.owner
        if (function.name != INVOKE) return
        val functionClass = function.parent as? IrClass ?: return
        val updatedClass = calculateUpdatedClass(functionClass) ?: return
        val newFunction = updatedClass.functions.firstOrNull { it.name == INVOKE } ?: return
        call.symbol = newFunction.symbol
    }

    private fun IrType.update(): IrType {
        return substitutor.substitute(this)
    }

    private val substitutor = TypeSubstitutor()

    private inner class TypeSubstitutor : AbstractIrTypeSubstitutor() {
        override fun substitute(type: IrType): IrType {
            if (type !is IrSimpleTypeImpl) return type
            val newArguments = type.arguments.map { substituteArgument(it) }
            val newClassifier = calculateUpdatedClassifier(type.classifier) ?: type.classifier
            return IrSimpleTypeImpl(
                newClassifier,
                type.nullability,
                newArguments,
                type.annotations,
                type.abbreviation
            )
        }

        private fun substituteArgument(typeArgument: IrTypeArgument): IrTypeArgument {
            return when (typeArgument) {
                is IrStarProjection -> typeArgument
                is IrTypeProjectionImpl -> typeArgument
                is IrTypeBase -> substitute(typeArgument) as IrTypeBase
                else -> error("Unexpected type argument: $typeArgument")
            }
        }
    }

    private fun calculateUpdatedClass(irClass: IrClass): IrClass? {
        return calculateUpdatedClassifier(irClass.symbol)?.owner as? IrClass
    }

    private fun calculateUpdatedClassifier(classifier: IrClassifierSymbol): IrClassifierSymbol? {
        val irClass = classifier.owner as? IrClass ?: return null
        val fqName = irClass.fqNameWhenAvailable ?: return null
        val fqNameString = fqName.asString()
        if (!fqNameString.startsWith(FULL_COMPOSABLE_NAME_PREFIX)) return null
        val number = fqNameString.removePrefix(FULL_COMPOSABLE_NAME_PREFIX).toIntOrNull() ?: return null
        val builtinClassId = FunctionTypeKind.Function.run {
            ClassId(packageFqName, Name.identifier("$classNamePrefix$number"))
        }
        return pluginContext.referenceClass(builtinClassId)
    }
}
