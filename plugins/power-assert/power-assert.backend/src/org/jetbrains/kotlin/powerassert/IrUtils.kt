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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
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
