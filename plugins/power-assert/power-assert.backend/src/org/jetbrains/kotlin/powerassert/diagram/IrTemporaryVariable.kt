/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2021-2023 Brian Norman
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

package org.jetbrains.kotlin.powerassert.diagram

import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

data class IrTemporaryVariable(
    val temporary: IrVariable,
    val original: IrExpression,
    val sourceRangeInfo: SourceRangeInfo,
    val text: String,
)

class IrTemporaryExtractionTransformer(
    private val builder: IrStatementsBuilder<*>,
    variables: List<IrTemporaryVariable>,
) : IrElementTransformerVoid() {
    private val replacements = variables.associate { it.original to it.temporary }

    override fun visitExpression(expression: IrExpression): IrExpression {
        val replacement = replacements[expression.attributeOwnerId]
        return if (replacement != null) {
            builder.irGet(replacement)
        } else {
            super.visitExpression(expression)
        }
    }
}
