/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType

class ClassLayoutBuilderFieldInfo(
    val name: String,
    val type: IrType,
    val isConst: Boolean,
    val irFieldSymbol: IrFieldSymbol,
    val alignment: Int,
) {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    val irField: IrField?
        get() = if (irFieldSymbol.isBound) irFieldSymbol.owner else null

    init {
        require(alignment.countOneBits() == 1) { "Alignment should be power of 2" }
    }
}