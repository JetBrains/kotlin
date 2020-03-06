/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.compiler.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlinx.stm.compiler.ATOMIC_FUNCTION_ANNOTATION
import org.jetbrains.kotlinx.stm.compiler.SHARED_MUTABLE_ANNOTATION
import org.jetbrains.kotlinx.stm.compiler.backend.ir.StmIrGenerator

private fun FunctionDescriptor.isAtomicFunction() = this.annotations.hasAnnotation(ATOMIC_FUNCTION_ANNOTATION)

private fun ClassDescriptor.isSharedClass() = this.annotations.hasAnnotation(SHARED_MUTABLE_ANNOTATION)

internal typealias FunctionTransformMap = HashMap<IrFunctionSymbol, IrFunction>

private class StmSharedClassLowering(
    val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.descriptor.isSharedClass())
            StmIrGenerator.patchSharedClass(
                declaration,
                pluginContext,
                pluginContext.symbolTable
            )

        declaration.transformChildrenVoid()

        return declaration
    }
}

private class StmAtomicFunctionLowering(
    val pluginContext: IrPluginContext,
    val resultMap: FunctionTransformMap
) : IrElementTransformerVoid() {

    private val argumentMap = hashMapOf<IrValueSymbol, IrValueParameter>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val result = if (declaration.descriptor.isAtomicFunction())
            StmIrGenerator.patchFunction(declaration, pluginContext, argumentMap).also {
                resultMap[declaration.symbol] = it
            }
        else
            declaration

        result.transformChildrenVoid(this)

        return result
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        expression.transformChildrenVoid(this)

        return argumentMap[expression.symbol]?.let { StmIrGenerator.patchGetUpdatedValue(expression, it) } ?: expression
    }

}

private class StmCallLowering(
    val pluginContext: IrPluginContext,
    val funTransformMap: FunctionTransformMap
) : IrElementTransformerVoid() {

    private val functionStack = mutableListOf<IrFunction>()

    override fun visitFunction(declaration: IrFunction): IrStatement {
        functionStack += declaration
        declaration.transformChildrenVoid(this)
        functionStack.pop()

        return declaration
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol.descriptor
        val containingDecl = callee.containingDeclaration

        val res = if (containingDecl is ClassDescriptor && containingDecl.isSharedClass() && callee is PropertyAccessorDescriptor)
            StmIrGenerator.patchPropertyAccess(expression, callee, functionStack, pluginContext.symbolTable, pluginContext)
        else if (callee.isAtomicFunction())
            StmIrGenerator.patchAtomicFunctionCall(expression, expression.symbol, functionStack, funTransformMap)
        else
            expression

        return res
    }

}

open class StmLoweringExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val funTransformMap = FunctionTransformMap()

        val stmFunctionLowering = StmAtomicFunctionLowering(pluginContext, funTransformMap)
        val stmClassLowering = StmSharedClassLowering(pluginContext)
        val stmCallLowering = StmCallLowering(pluginContext, funTransformMap)

        // apply in order:
        arrayOf(stmFunctionLowering, stmClassLowering, stmCallLowering).forEach { lowering ->
            moduleFragment.files.forEach { file ->
                file.accept(lowering, null)
            }
        }
    }
}