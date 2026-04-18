/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class LombokToStringIrBodyFiller(private val context: IrPluginContext) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val toStringGeneratorKey = (declaration.origin as? GeneratedByPlugin)?.pluginKey as? ToStringGeneratorKey

        if (toStringGeneratorKey != null) {
            declaration.body = DeclarationIrBuilder(context, declaration.symbol).irBlockBody {
                val thisParam = declaration.dispatchReceiverParameter!!
                +irReturn(buildToStringExpression(declaration.parent as IrClass, toStringGeneratorKey, thisParam))
            }
        }

        declaration.acceptChildrenVoid(this)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBuilderWithScope.buildToStringExpression(
        irClass: IrClass,
        key: ToStringGeneratorKey,
        thisParam: IrValueParameter,
    ): IrExpression {
        if (key.propertyInfos.isEmpty()) {
            return irString("${key.className}()")
        }

        return irConcat().also { concat ->
            concat.arguments.apply {
                add(irString("${key.className}("))

                for ((index, propInfo) in key.propertyInfos.withIndex()) {
                    if (index > 0) {
                        add(irString(", "))
                    }

                    if (propInfo.displayName != null) {
                        add(irString("${propInfo.displayName}="))
                    }

                    val getter = irClass.getPropertyGetter(propInfo.propertyName)!!

                    add(irCall(getter).apply {
                        arguments[0] =
                            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, thisParam.type, thisParam.symbol)
                    })
                }

                add(irChar(')'))
            }
        }
    }
}
