/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit

private fun PrimitiveBinaryType?.toLlvmType(llvm: CodegenLlvmHelpers) = when (this) {
    null -> llvm.kObjHeaderPtr

    PrimitiveBinaryType.BOOLEAN -> llvm.int1Type
    PrimitiveBinaryType.BYTE -> llvm.int8Type
    PrimitiveBinaryType.SHORT -> llvm.int16Type
    PrimitiveBinaryType.INT -> llvm.int32Type
    PrimitiveBinaryType.LONG -> llvm.int64Type
    PrimitiveBinaryType.FLOAT -> llvm.floatType
    PrimitiveBinaryType.DOUBLE -> llvm.doubleType

    PrimitiveBinaryType.VECTOR128 -> llvm.vector128Type
    PrimitiveBinaryType.POINTER -> llvm.int8PtrType
}

internal fun IrType.toLLVMType(llvm: CodegenLlvmHelpers): LLVMTypeRef =
        llvm.runtime.calculatedLLVMTypes.getOrPut(this) { computePrimitiveBinaryTypeOrNull().toLlvmType(llvm) }

internal fun IrType.isVoidAsReturnType() = isUnit() || isNothing()

internal fun IrType.getLLVMReturnType(llvm: CodegenLlvmHelpers) = when {
    isVoidAsReturnType() -> llvm.voidType
    else -> toLLVMType(llvm)
}
