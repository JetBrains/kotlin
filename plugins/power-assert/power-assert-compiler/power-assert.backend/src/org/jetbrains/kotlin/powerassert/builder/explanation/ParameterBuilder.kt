/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert.builder.explanation

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.powerassert.diagram.*

interface ParameterBuilder {
    fun build(
        builder: IrBuilderWithScope,
        argumentVariables: Map<IrValueParameter, List<IrTemporaryVariable>>,
    ): IrExpression
}

