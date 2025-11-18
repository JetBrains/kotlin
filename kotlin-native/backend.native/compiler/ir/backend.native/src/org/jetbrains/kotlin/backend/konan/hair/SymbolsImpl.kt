/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.sym.HairFunction
import hair.sym.HairType
import org.jetbrains.kotlin.backend.konan.BinaryType
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.computeBinaryType
import org.jetbrains.kotlin.backend.konan.computePrimitiveBinaryTypeOrNull
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType

internal fun IrType.asHairType(): HairType = when (val binaryType = computePrimitiveBinaryTypeOrNull()) {
    PrimitiveBinaryType.BOOLEAN -> HairType.BOOLEAN
    PrimitiveBinaryType.BYTE,
    PrimitiveBinaryType.SHORT,
    PrimitiveBinaryType.INT -> HairType.INT
    PrimitiveBinaryType.LONG -> HairType.LONG
    PrimitiveBinaryType.FLOAT -> HairType.FLOAT
    PrimitiveBinaryType.DOUBLE -> HairType.DOUBLE
    PrimitiveBinaryType.POINTER -> error("Should not reach here")
    PrimitiveBinaryType.VECTOR128 -> TODO("$binaryType not implemented yet")
    null -> HairType.REFERENCE
}

internal data class HairFunctionImpl(val irFunction: IrSimpleFunction) : HairFunction {
    override fun toString() = irFunction.name.toString()
    override val resultHairType: HairType
        get() = irFunction.returnType.asHairType()
}
