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

package org.jetbrains.kotlin.powerassert.delegate

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

class SimpleFunctionDelegate(
    private val overload: IrSimpleFunctionSymbol,
    override val messageParameter: IrValueParameter,
) : FunctionDelegate {
    override val function = overload.owner

    override fun buildCall(
        builder: IrBuilderWithScope,
        original: IrCall,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArguments: List<IrExpression?>,
        messageArgument: IrExpression,
    ): IrExpression = builder.irCallCopy(
        overload = overload,
        original = original,
        dispatchReceiver = dispatchReceiver,
        extensionReceiver = extensionReceiver,
        valueArguments = valueArguments,
        messageArgument = messageArgument,
    )
}
