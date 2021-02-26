/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit

private fun RuntimeAware.getLlvmType(primitiveBinaryType: PrimitiveBinaryType?) = when (primitiveBinaryType) {
    null -> this.kObjHeaderPtr

    PrimitiveBinaryType.BOOLEAN -> int1Type
    PrimitiveBinaryType.BYTE -> int8Type
    PrimitiveBinaryType.SHORT -> int16Type
    PrimitiveBinaryType.INT -> int32Type
    PrimitiveBinaryType.LONG -> int64Type
    PrimitiveBinaryType.FLOAT -> floatType
    PrimitiveBinaryType.DOUBLE -> doubleType

    PrimitiveBinaryType.VECTOR128 -> vector128Type
    PrimitiveBinaryType.POINTER -> int8TypePtr
}

internal fun RuntimeAware.getLLVMType(type: IrType): LLVMTypeRef =
        runtime.calculatedLLVMTypes.getOrPut(type) { getLlvmType(type.computePrimitiveBinaryTypeOrNull()) }

internal fun RuntimeAware.getLLVMType(type: DataFlowIR.Type) =
        getLlvmType(type.primitiveBinaryType)

internal fun IrType.isVoidAsReturnType() = isUnit() || isNothing()

internal fun RuntimeAware.getLLVMReturnType(type: IrType): LLVMTypeRef {
    return when {
        type.isVoidAsReturnType() -> voidType
        else -> getLLVMType(type)
    }
}
