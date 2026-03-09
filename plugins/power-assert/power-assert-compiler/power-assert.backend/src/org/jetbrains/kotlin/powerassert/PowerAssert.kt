/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.powerassert

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.name.FqName

data object PowerAssertTemporary : GeneratedDeclarationKey()
data object FunctionForPowerAssert : GeneratedDeclarationKey()

val POWER_ASSERT_BLOCK by IrStatementOriginImpl
val POWER_ASSERT_TEMPORARY = IrDeclarationOrigin.GeneratedByPlugin(PowerAssertTemporary)
val FUNCTION_FOR_POWER_ASSERT = IrDeclarationOrigin.GeneratedByPlugin(FunctionForPowerAssert)

val PowerAssertGetExplanation = FqName("kotlinx.powerassert.PowerAssert.Companion.<get-explanation>")
