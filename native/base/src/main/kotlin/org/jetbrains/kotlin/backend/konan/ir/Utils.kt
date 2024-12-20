/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.utils.atMostOne

fun IrClass.isClassTypeWithSignature(signature: IdSignature.CommonSignature): Boolean {
    return signature == symbol.signature
}
val IrClass.superClasses get() = this.superTypes.map { it.classifierOrFail as IrClassSymbol }
@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrClass.getSuperClassNotAny() = this.superClasses.map { it.owner }.atMostOne { !it.isInterface && !it.isAny() }

fun IrClass.isAny() = this.isClassTypeWithSignature(IdSignatureValues.any)
