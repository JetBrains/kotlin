/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2020-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.BuiltInOperatorNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.isComparisonOperator
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name

fun IrBuilderWithScope.irString(builderAction: StringBuilder.() -> Unit) =
    irString(buildString { builderAction() })

fun IrBuilderWithScope.irLambda(
    returnType: IrType,
    lambdaType: IrType,
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    block: IrBlockBodyBuilder.() -> Unit,
): IrFunctionExpression {
    val scope = this
    val lambda = context.irFactory.buildFun {
        this.startOffset = startOffset
        this.endOffset = endOffset
        name = Name.special("<anonymous>")
        this.returnType = returnType
        visibility = DescriptorVisibilities.LOCAL
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    }.apply {
        val bodyBuilder = DeclarationIrBuilder(context, symbol, startOffset, endOffset)
        body = bodyBuilder.irBlockBody {
            block()
        }
        parent = scope.parent
    }
    return IrFunctionExpressionImpl(startOffset, endOffset, lambdaType, lambda, IrStatementOrigin.LAMBDA)
}

val IrElement.earliestStartOffset: Int
    get() {
        var offset = startOffset
        this.acceptChildrenVoid(
            object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    if (element.startOffset < offset) offset = element.startOffset
                    element.acceptChildrenVoid(this)
                }
            },
        )
        return offset
    }

/**
 * An implicit argument may only be either `this` class dispatch receiver ([IrGetField])
 * or `this` extension function receiver ([IrGetValue]). As such, these are the only two
 * IR elements that need to be checked for [IrStatementOrigin.IMPLICIT_ARGUMENT].
 */
internal fun IrExpression.isImplicitArgument(): Boolean = when (this) {
    is IrGetValue -> origin == IrStatementOrigin.IMPLICIT_ARGUMENT
    is IrGetField -> origin == IrStatementOrigin.IMPLICIT_ARGUMENT
    else -> false
}

internal fun IrCall.getExplicitReceiver(): IrExpression? {
    for (parameter in symbol.owner.parameters) {
        if (parameter.kind == IrParameterKind.DispatchReceiver || parameter.kind == IrParameterKind.ExtensionReceiver) {
            val argument = arguments[parameter] ?: continue
            if (!argument.isImplicitArgument()) return argument
        }
    }
    return null
}

internal fun IrCall.isInnerOfNotEqualOperator(): Boolean =
    (origin == IrStatementOrigin.EXCLEQ && symbol.owner.name.asString() == BuiltInOperatorNames.EQEQ) ||
            (origin == IrStatementOrigin.EXCLEQEQ && symbol.owner.name.asString() == BuiltInOperatorNames.EQEQEQ)

internal fun IrCall.isInnerOfComparisonOperator(): Boolean =
    origin?.isComparisonOperator() == true && symbol.owner.name.asString() == BuiltInOperatorNames.COMPARE_TO
