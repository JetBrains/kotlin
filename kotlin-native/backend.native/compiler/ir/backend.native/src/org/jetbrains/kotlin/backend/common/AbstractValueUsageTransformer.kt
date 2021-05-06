/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


/**
 * Transforms expressions depending on the context they are used in.
 *
 * The transformations are defined with `IrExpression.use*` methods in this class,
 * the most common are [useAs], [useAsStatement], [useInTypeOperator].
 *
 * TODO: the implementation is originally based on [org.jetbrains.kotlin.psi2ir.transformations.InsertImplicitCasts]
 * and should probably be used as its base.
 *
 * TODO: consider making this visitor non-recursive to make it more general.
 */
internal abstract class AbstractValueUsageTransformer(
        val builtIns: KotlinBuiltIns,
        val symbols: KonanSymbols,
        val irBuiltIns: IrBuiltIns
): IrElementTransformerVoid() {

    protected open fun IrExpression.useAs(type: IrType): IrExpression = this

    protected open fun IrExpression.useAsStatement(): IrExpression = this

    protected open fun IrExpression.useInTypeOperator(operator: IrTypeOperator, typeOperand: IrType): IrExpression =
            this

    protected open fun IrExpression.useAsValue(value: IrValueDeclaration): IrExpression = this.useAs(value.type)

    protected open fun IrExpression.useAsArgument(parameter: IrValueParameter): IrExpression {
        return this.useAsValue(parameter)
    }

    protected open fun IrExpression.useAsDispatchReceiver(expression: IrFunctionAccessExpression): IrExpression =
            this.useAsArgument(expression.symbol.owner.dispatchReceiverParameter!!)

    protected open fun IrExpression.useAsExtensionReceiver(expression: IrFunctionAccessExpression): IrExpression =
            this.useAsArgument(expression.symbol.owner.extensionReceiverParameter!!)

    protected open fun IrExpression.useAsValueArgument(expression: IrFunctionAccessExpression,
                                                       parameter: IrValueParameter
    ): IrExpression =
            this.useAsArgument(parameter)

    private fun IrExpression.useForVariable(variable: IrVariable): IrExpression =
            this.useAsValue(variable)

    private fun IrExpression.useForValue(value: IrValueDeclaration) =
            this.useAsValue(value)

    private fun IrExpression.useForField(field: IrField): IrExpression =
            this.useAs(field.type)

    protected open fun IrExpression.useAsReturnValue(returnTarget: IrReturnTargetSymbol): IrExpression =
            when (returnTarget) {
                is IrSimpleFunctionSymbol -> this.useAs(returnTarget.owner.returnType)
                is IrConstructorSymbol -> this.useAs(irBuiltIns.unitType)
                is IrReturnableBlockSymbol -> this.useAs(returnTarget.owner.type)
                else -> error(returnTarget)
            }

    protected open fun IrExpression.useAsResult(enclosing: IrExpression): IrExpression =
            this.useAs(enclosing.type)

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        TODO()
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
        TODO()
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        TODO()
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        expression.transformChildrenVoid(this)

        with(expression) {
            dispatchReceiver = dispatchReceiver?.useAsDispatchReceiver(expression)
            extensionReceiver = extensionReceiver?.useAsExtensionReceiver(expression)
            for (index in symbol.owner.valueParameters.indices) {
                val argument = getValueArgument(index) ?: continue
                val parameter = symbol.owner.valueParameters[index]
                putValueArgument(index, argument.useAsValueArgument(expression, parameter))
            }
        }

        return expression
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        body.transformChildrenVoid(this)

        body.statements.forEachIndexed { i, irStatement ->
            if (irStatement is IrExpression) {
                body.statements[i] = irStatement.useAsStatement()
            }
        }

        return body
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        expression.transformChildrenVoid(this)

        if (expression.statements.isEmpty()) {
            return expression
        }

        val lastIndex = expression.statements.lastIndex
        expression.statements.forEachIndexed { i, irStatement ->
            if (irStatement is IrExpression) {
                expression.statements[i] =
                        if (i == lastIndex)
                            irStatement.useAsResult(expression)
                        else
                            irStatement.useAsStatement()
            }
        }

        return expression
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useAsReturnValue(expression.returnTargetSymbol)

        return expression
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useForValue(expression.symbol.owner)

        return expression
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useForField(expression.symbol.owner)

        return expression
    }

    override fun visitField(declaration: IrField): IrStatement {
        declaration.transformChildrenVoid(this)

        declaration.initializer?.let {
            it.expression = it.expression.useForField(declaration)
        }

        return declaration
    }

    override fun visitVariable(declaration: IrVariable): IrVariable {
        declaration.transformChildrenVoid(this)

        declaration.initializer = declaration.initializer?.useForVariable(declaration)

        return declaration
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        expression.transformChildrenVoid(this)

        for (irBranch in expression.branches) {
            irBranch.condition = irBranch.condition.useAs(irBuiltIns.booleanType)
            irBranch.result = irBranch.result.useAsResult(expression)
        }

        return expression
    }

    override fun visitLoop(loop: IrLoop): IrExpression {
        loop.transformChildrenVoid(this)

        loop.condition = loop.condition.useAs(irBuiltIns.booleanType)

        loop.body = loop.body?.useAsStatement()

        return loop
    }

    override fun visitThrow(expression: IrThrow): IrExpression {
        expression.transformChildrenVoid(this)

        expression.value = expression.value.useAs(symbols.throwable.owner.defaultType)

        return expression
    }

    override fun visitTry(aTry: IrTry): IrExpression {
        aTry.transformChildrenVoid(this)

        aTry.tryResult = aTry.tryResult.useAsResult(aTry)

        for (aCatch in aTry.catches) {
            aCatch.result = aCatch.result.useAsResult(aTry)
        }

        aTry.finallyExpression = aTry.finallyExpression?.useAsStatement()

        return aTry
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        expression.transformChildrenVoid(this)

        expression.elements.forEachIndexed { i, element ->
            when (element) {
                is IrSpreadElement ->
                    element.expression = element.expression.useAs(expression.type)
                is IrExpression ->
                    expression.putElement(i, element.useAs(expression.varargElementType))
            }
        }

        return expression
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid(this)

        expression.argument = expression.argument.useInTypeOperator(expression.operator, expression.typeOperand)

        return expression
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        declaration.transformChildrenVoid(this)

        declaration.valueParameters.forEach { parameter ->
            val defaultValue = parameter.defaultValue
            if (defaultValue is IrExpressionBody) {
                defaultValue.expression = defaultValue.expression.useAsArgument(parameter)
            }
        }

        declaration.body?.let {
            if (it is IrExpressionBody) {
                it.expression = it.expression.useAsReturnValue(declaration.symbol)
            }
        }

        return declaration
    }

    // TODO: IrStringConcatenation, IrEnumEntry?

}

