/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

val IrClassifierSymbol.typeWithoutArguments: IrType
    get() = when (this) {
        is IrClassSymbol -> {
            require(this.descriptor.declaredTypeParameters.isEmpty())
            this.typeWith(arguments = emptyList())
        }
        is IrTypeParameterSymbol -> this.defaultType
        else -> error(this)
    }

val IrClassifierSymbol.typeWithStarProjections
    get() = when (this) {
        is IrClassSymbol -> createType(
                hasQuestionMark = false,
                arguments = this.descriptor.declaredTypeParameters.map { IrStarProjectionImpl }
        )
        is IrTypeParameterSymbol -> this.defaultType
        else -> error(this)
    }

val IrTypeParameterSymbol.defaultType: IrType get() =  IrSimpleTypeImpl(
        this,
        false,
        emptyList(),
        emptyList()
)

fun IrClass.typeWith(arguments: List<IrType>) = this.symbol.typeWith(arguments)

fun IrType.containsNull(): Boolean = if (this is IrSimpleType) {
    if (this.hasQuestionMark) {
        true
    } else {
        val classifier = this.classifier
        when (classifier) {
            is IrClassSymbol -> false
            is IrTypeParameterSymbol -> classifier.owner.superTypes.any { it.containsNull() }
            else -> error(classifier)
        }
    }
} else {
    true
}

// TODO: get rid of these:
fun IrType.isSubtypeOf(other: KotlinType): Boolean = this.toKotlinType().isSubtypeOf(other)
fun IrType.isSubtypeOf(other: IrType): Boolean = this.isSubtypeOf(other.toKotlinType())
