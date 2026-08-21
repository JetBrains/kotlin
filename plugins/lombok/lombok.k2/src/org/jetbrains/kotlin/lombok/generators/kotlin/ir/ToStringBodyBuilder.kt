/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin.ir

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irChar
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.lombok.generators.ToStringGeneratorKey
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object ToStringBodyBuilder : IrBodyBuilder<ToStringGeneratorKey>() {
    private val DEEP_TO_STRING_NAME = Name.identifier("deepToString")

    override fun IrBlockBodyBuilder.build(key: ToStringGeneratorKey, declaration: IrSimpleFunction) {
        val thisParam = declaration.dispatchReceiverParameter!!
        +irReturn(buildToStringExpression(declaration.parent as IrClass, key, thisParam))
    }

    private fun IrBuilderWithScope.buildToStringExpression(
        irClass: IrClass,
        key: ToStringGeneratorKey,
        thisParam: IrValueParameter,
    ): IrExpression {
        val superToStringCall = runIf(key.callSuper) { buildSuperToStringCall(irClass, thisParam) }

        if (superToStringCall == null && key.propertyInfos.isEmpty()) {
            return irString("${key.className}()")
        }

        return irConcat().apply {
            addArgument(irString("${key.className}("))

            if (superToStringCall != null) {
                addArgument(irString("super="))
                addArgument(superToStringCall)
            }

            // Not the loop index: a property may be skipped below, and then it owes no separator to the next one.
            var isFirstRendered = superToStringCall == null

            for ([propertyName, displayName, ignoreWithoutBackingField] in key.propertyInfos) {
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                val propertyDeclaration = irClass.findDeclaration<IrProperty> { it.name == propertyName }

                if (propertyDeclaration == null || (propertyDeclaration.backingField == null && ignoreWithoutBackingField)) {
                    continue
                }

                addArgument(
                    irString(
                        buildString {
                            if (!isFirstRendered) {
                                append(", ")
                            }

                            if (displayName != null) {
                                append("$displayName=")
                            }
                        }
                    )
                )
                isFirstRendered = false

                val getter = propertyDeclaration.getter!!
                val value = irCall(getter.symbol).apply {
                    arguments[0] =
                        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParam.type, thisParam.symbol)
                }

                addArgument(renderArrayByContent(value, getter.returnType) ?: value)
            }

            addArgument(irChar(')'))
        }
    }

    /** Renders an array property by content, falling back to plain concatenation for anything else. */
    private fun IrBuilderWithScope.renderArrayByContent(value: IrExpression, type: IrType): IrExpression? {
        val toStringFunction =
            findArraysFunctionByContent(type, OperatorNameConventions.TO_STRING, DEEP_TO_STRING_NAME, parameterCount = 1)
                ?: return null

        return irCall(toStringFunction.symbol).apply { arguments[0] = value }
    }

    /**
     * The `super.toString()` call a `callSuper` key asks for, with no say in whether it belongs there:
     * `shouldCallSuper` has already decided, `Any` included. Lombok honors an explicit `callSuper = true` on a
     * direct descendant of `Object` too, rendering the identity hash its `toString` returns - "pretty much
     * meaningless", as `@ToString` puts it, but asked for.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildSuperToStringCall(
        irClass: IrClass,
        thisParam: IrValueParameter,
    ): IrExpression? {
        val superClass = irClass.superTypes
            .firstNotNullOfOrNull { type -> type.classOrNull?.owner?.takeIf { !it.isInterface } }
            ?: return null

        val superToStringFun = superClass.functions
            .firstOrNull {
                it.name == OperatorNameConventions.TO_STRING &&
                        it.parameters.singleOrNull { p -> p.kind == IrParameterKind.DispatchReceiver } != null
            } ?: return null

        return IrCallImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = superToStringFun.returnType,
            symbol = superToStringFun.symbol,
            typeArgumentsCount = 0,
            superQualifierSymbol = superClass.symbol,
        ).apply {
            arguments[0] = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParam.type, thisParam.symbol)
        }
    }
}
