/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.common

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.substitute

typealias TypeSubstitution = Map<IrTypeParameterSymbol, IrType>

fun TypeSubstitution.composition(other: TypeSubstitution): TypeSubstitution {
    return mapValues { (_, tp) -> tp.substitute(other) }
}

private fun IrType.isPure(): Boolean {
    return when (this) {
        is IrSimpleType -> classOrNull != null && arguments.isEmpty()
        else -> false
    }
}

private fun TypeSubstitution.isPure(): Boolean {
    return values.all { it.isPure() }
}

private fun TypeSubstitution.isComplete(function: IrSimpleFunction): Boolean {
    return function.typeParameters.filter { it.hasAnnotation(FqnUtils.MONOMORPHIC_ANNOTATION_FQN) }.all {
        it.symbol in keys
    }
}

fun TypeSubstitution.isRightMonomorphic(function: IrSimpleFunction): Boolean {
    return isNotEmpty() && isPure() && isComplete(function)
}

fun TypeSubstitution.toRightMonomorphic(function: IrSimpleFunction): TypeSubstitution? {
    val result = filter { (param, _) -> param.owner.hasAnnotation(FqnUtils.MONOMORPHIC_ANNOTATION_FQN) }
    return result.takeIf {
        it.isRightMonomorphic(function)
    }
}