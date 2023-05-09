/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.computePrimitiveBinaryTypeOrNull
import org.jetbrains.kotlin.ir.types.IrType

internal class NativeDefaultArgumentFunctionFactory(context: CommonBackendContext) : MaskedDefaultArgumentFunctionFactory(context) {
    override fun IrType.hasNullAsUndefinedValue(): Boolean {
        val binaryType = computePrimitiveBinaryTypeOrNull() ?: return true
        return binaryType == PrimitiveBinaryType.POINTER || binaryType == PrimitiveBinaryType.VECTOR128
    }
}