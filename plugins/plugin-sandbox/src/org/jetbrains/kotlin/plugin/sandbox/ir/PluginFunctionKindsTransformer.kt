/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.plugin.sandbox.fir.types.PluginFunctionalNames.FULL_INLINEABLE_NAME_PREFIX
import org.jetbrains.kotlin.plugin.sandbox.fir.types.PluginFunctionalNames.FULL_NOT_INLINEABLE_NAME_PREFIX
import org.jetbrains.kotlin.plugin.sandbox.fir.types.PluginFunctionalNames.INLINEABLE_NAME_PREFIX
import org.jetbrains.kotlin.plugin.sandbox.fir.types.PluginFunctionalNames.NOT_INLINEABLE_NAME_PREFIX

class PluginFunctionKindsTransformer(val pluginContext: IrPluginContext) : IrElementVisitorVoid {
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

    /**
     * This function propagates the special function type kind for composable to function expressions like lambda expression.
     */
    override fun visitFunctionExpression(expression: IrFunctionExpression) {
        when {
            expression.type.isInlineableFunction() -> expression.function.mark(inlineable = true)
            expression.type.isNotInlineableFunction() -> expression.function.mark(inlineable = false)
        }
        super.visitFunctionExpression(expression)
    }

    /**
     * This function propagates the special function type kind for composable to function references.
     */
    override fun visitFunctionReference(expression: IrFunctionReference) {
        when {
            expression.type.isInlineableFunction() -> expression.symbol.owner.mark(inlineable = true)
            expression.type.isNotInlineableFunction() -> expression.symbol.owner.mark(inlineable = false)
        }
        super.visitFunctionReference(expression)
    }

    private fun IrType.isInlineableFunction(): Boolean = isSyntheticSandboxFunction(INLINEABLE_NAME_PREFIX)
    private fun IrType.isNotInlineableFunction(): Boolean = isSyntheticSandboxFunction(NOT_INLINEABLE_NAME_PREFIX)

    private fun IrType.isSyntheticSandboxFunction(name: String): Boolean {
        return classOrNull?.owner?.let {
            it.name.asString().startsWith(name) && it.packageFqName?.asString() == "some"
        } ?: false
    }

    private val inlineableClassId = ClassId(FqName("org.jetbrains.kotlin.plugin.sandbox"), FqName("MyInlineable"), false)
    private val notInlineableClassId = ClassId(FqName("org.jetbrains.kotlin.plugin.sandbox"), FqName("MyNotInlineable"), false)

    private val inlineableSymbol = pluginContext.referenceClass(inlineableClassId)!!
    private val notInlineableSymbol = pluginContext.referenceClass(notInlineableClassId)!!

    private fun IrFunction.mark(inlineable: Boolean) {
        if (!hasAnnotation(inlineableClassId)) {
            val symbol = when {
                inlineable -> inlineableSymbol
                else -> notInlineableSymbol
            }
            annotations = annotations + IrConstructorCallImpl.fromSymbolOwner(
                symbol.owner.defaultType,
                symbol.constructors.single(),
            )
        }
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
                is IrType -> substitute(typeArgument)
                is IrTypeProjection -> typeArgument
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
        val prefixes = listOf(FULL_INLINEABLE_NAME_PREFIX, FULL_NOT_INLINEABLE_NAME_PREFIX)

        for (prefix in prefixes) {
            if (!fqNameString.startsWith(prefix)) continue
            val number = fqNameString.removePrefix(prefix).toIntOrNull() ?: continue
            val builtinClassId = FunctionTypeKind.Function.run {
                ClassId(packageFqName, Name.identifier("$classNamePrefix$number"))
            }
            return pluginContext.referenceClass(builtinClassId)
        }
        return null
    }
}
