/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.FunctionMetrics
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.irFlag

internal var IrDeclaration.isDefaultParamStub: Boolean by irFlag(copyByDefault = true)

internal var IrFunctionAccessExpression.associatedComposableSingletonStub: IrCall? by irAttribute(copyByDefault = true)

internal var IrSimpleFunction.isVirtualFunctionWithDefaultParam: Boolean? by irAttribute(copyByDefault = true)

internal var IrFunction.functionMetrics: FunctionMetrics? by irAttribute(copyByDefault = true)

internal var IrFunction.isComposableReferenceAdapter: Boolean by irFlag(copyByDefault = true)