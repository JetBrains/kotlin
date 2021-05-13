/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import androidx.compose.compiler.plugins.kotlin.isMarkedAsComposable
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.util.typeSubstitutionMap
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

fun IrModuleFragment.annotateComposableFunctions(pluginContext: IrPluginContext):
    Collection<IrAttributeContainer> {
        return IrComposableAnnotator(pluginContext)
            .apply { this@annotateComposableFunctions.acceptChildrenVoid(this) }
            .composables
    }

private class IrComposableAnnotator(val pluginContext: IrPluginContext) : IrElementVisitorVoid {

    val composables = mutableSetOf<IrAttributeContainer>()
    val expectedComposable = mutableMapOf<IrElement, Boolean>()
    val expectedReturnComposable = mutableMapOf<FunctionDescriptor, Boolean>()

    @ObsoleteDescriptorBasedAPI
    override fun visitFunction(declaration: IrFunction) {

        expectedReturnComposable.put(
            declaration.descriptor,
            declaration.returnType.toKotlinType().hasComposableAnnotation()
        )

        if (expectedComposable.get(declaration) == true) {
            declaration.setComposableAnnotation()
        }

        if (declaration.hasComposableAnnotation())
            composables.add((declaration as IrAttributeContainer).attributeOwnerId)

        super.visitFunction(declaration)
    }

    private fun IrFunction.setComposableAnnotation() {
        if (hasComposableAnnotation()) return
        val composableAnnotation = pluginContext.referenceClass(ComposeFqNames.Composable)!!.owner
        annotations = annotations + listOf(
            IrConstructorCallImpl.fromSymbolOwner(
                type = composableAnnotation.defaultType,
                constructorSymbol = composableAnnotation.constructors.first().symbol
            )
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitField(declaration: IrField) {
        declaration.initializer?.let { initializer ->
            expectedComposable.put(
                initializer,
                declaration.type.toKotlinType().hasComposableAnnotation()
            )
        }
        super.visitField(declaration)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall) {
        val declaration = expression.symbol.owner
        if (declaration.hasComposableAnnotation())
            composables.add(declaration.attributeOwnerId)

        val irFunction = expression.symbol.owner
        irFunction.valueParameters.forEachIndexed { index, it ->
            val arg = expression.getValueArgument(index)
            if (arg != null) {
                val parameter = it.type.substitute(expression.typeSubstitutionMap)
                val isComposable = parameter.hasComposableAnnotation()
                expectedComposable[arg] = isComposable
            }
        }
        super.visitCall(expression)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitConstructorCall(expression: IrConstructorCall) {
        val irFunction = expression.symbol.owner
        irFunction.valueParameters.forEachIndexed { index, it ->
            val arg = expression.getValueArgument(index)
            if (arg != null) {
                val parameter = it.type.substitute(expression.typeSubstitutionMap)
                val isComposable = parameter.hasComposableAnnotation()
                expectedComposable[arg] = isComposable
            }
        }
        super.visitConstructorCall(expression)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitValueParameter(declaration: IrValueParameter) {
        declaration.defaultValue?.let { defaultValue ->
            expectedComposable.put(
                defaultValue,
                declaration.type.toKotlinType().hasComposableAnnotation()
            )
        }
        super.visitValueParameter(declaration)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitExpression(expression: IrExpression) {
        val expectedType = expectedComposable.get(expression)
        when (expression) {
            is IrFunctionExpression ->
                expectedComposable.put(
                    expression.function,
                    expression.type.hasComposableAnnotation()
                )
            is IrExpressionBody ->
                if (expectedType != null)
                    expectedComposable.put(expression.expression, expectedType)
            is IrReturn -> {
                val expectedReturnType = expectedReturnComposable.get(
                    expression.returnTargetSymbol.descriptor
                ) ?: false
                expectedComposable.put(expression.value, expectedReturnType)
            }
            is IrVararg -> {
                expression.elements.forEach {
                    expectedComposable.put(it, expression.type.hasComposableAnnotation())
                }
            }
        }
        super.visitExpression(expression)
    }

    override fun visitWhen(expression: IrWhen) {
        val expectedType = expectedComposable.get(expression)
        if (expectedType != null)
            expression.branches.forEach {
                expectedComposable.put(it.result, expectedType)
            }
        super.visitWhen(expression)
    }

    override fun visitBody(body: IrBody) {
        val expectedType = expectedComposable.get(body)
        when (body) {
            is IrExpressionBody ->
                if (expectedType != null)
                    expectedComposable.put(body.expression, expectedType)
        }
        super.visitBody(body)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitDeclaration(declaration: IrDeclarationBase) {
        when (declaration) {
            is IrProperty -> {
                declaration.getter?.let { getter ->
                    expectedComposable.put(getter, getter.descriptor.isMarkedAsComposable())
                }
                declaration.setter?.let { setter ->
                    expectedComposable.put(setter, setter.descriptor.isMarkedAsComposable())
                }
            }
            is IrVariable -> {
                declaration.initializer?.let { initializer ->
                    expectedComposable.put(
                        initializer,
                        declaration.type.toKotlinType().hasComposableAnnotation()
                    )
                }
            }
        }
        super.visitDeclaration(declaration)
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        val expectedType = expectedComposable.get(expression)
        if (expectedType != null && expression.statements.size > 0)
            expectedComposable.put(
                expression.statements.last(),
                expectedType
            )
        super.visitContainerExpression(expression)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}
