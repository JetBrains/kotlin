/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.IMPLICIT_CAST
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator.SAM_CONVERSION
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.parents
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.platform.jvm.isJvm

@Suppress("PRE_RELEASE_CLASS")
class ComposableFunInterfaceLowering(private val context: IrPluginContext) :
    IrElementTransformerVoidWithContext(),
    ModuleLoweringPass {

    override fun lower(irModule: IrModuleFragment) {
        irModule.transformChildrenVoid(this)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        val functionExpr = expression.findSamFunctionExpr()
        if (functionExpr != null && expression.typeOperand.isComposableFunInterface()) {
            val argument = functionExpr.transform(this, null) as IrFunctionExpression
            val superType = expression.typeOperand
            val superClass = superType.classOrNull ?: error("Expected non-null class")
            // A class on every backend: its method is a restartable function with its own group
            val isJvm = context.platform.isJvm()
            return FunctionReferenceBuilder(
                argument,
                superClass,
                superType,
                currentDeclarationParent!!,
                context,
                currentScope!!.scope.scopeOwnerSymbol,
                IrTypeSystemContextImpl(context.irBuiltIns),
                isJvm,
                // JVM makes a capture-free class a singleton itself
                singleton = !isJvm && !argument.function.referencesOuterDeclarations(superType),
            ).build()
        }
        return super.visitTypeOperator(expression)
    }
}

/**
 * Whether this function or [funInterfaceType] references a value, a local declaration or a type parameter declared outside this
 * function: then its class can't be an object.
 */
private fun IrFunction.referencesOuterDeclarations(funInterfaceType: IrType): Boolean {
    val function = this
    var referenced = false
    val visitor = object : IrTypeVisitorVoid() {
        override fun visitType(container: IrElement, type: IrType) {
            val typeParameter = type.classifierOrNull?.owner as? IrTypeParameter ?: return
            if (function !in typeParameter.parents) referenced = true
        }

        override fun visitDeclarationReference(expression: IrDeclarationReference) {
            val owner = expression.symbol.owner as? IrDeclaration
            if (owner != null && (owner is IrValueDeclaration || owner.isLocal) && function !in owner.parents) referenced = true
            super.visitDeclarationReference(expression)
        }
    }
    acceptVoid(visitor)
    visitor.visitTypeRecursively(this, funInterfaceType)
    return referenced
}

private fun IrType.isComposableFunInterface(): Boolean =
    classOrNull
        ?.functions
        ?.single { it.owner.modality == Modality.ABSTRACT }
        ?.owner
        ?.hasComposableAnnotation() == true

internal fun IrTypeOperatorCall.findSamFunctionExpr(): IrFunctionExpression? {
    val argument = argument
    val operator = operator
    val type = typeOperand
    val functionClass = type.classOrNull

    val isFunInterfaceConversion = operator == SAM_CONVERSION &&
            functionClass != null &&
            functionClass.owner.isFun

    return if (isFunInterfaceConversion) {
        // if you modify this logic, make sure to update wrapping of type operators
        // in ComposerLambdaMemoization.kt
        when {
            argument is IrFunctionExpression && argument.origin.isLambda -> argument
            // some expressions are wrapped with additional implicit cast operator
            // unwrapping that allows to avoid SAM conversion that capture FunctionN and box params.
            argument is IrTypeOperatorCall && argument.operator == IMPLICIT_CAST -> {
                val functionExpr = argument.argument
                functionExpr as? IrFunctionExpression
            }
            else -> null
        }
    } else {
        null
    }
}
