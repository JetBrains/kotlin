/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.cexport

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

internal sealed interface CAdapterCodegenElement {
    val symbol: IrSymbol
    val exportedElement: ExportedElement

    class Function(
        override val symbol: IrFunctionSymbol,
        override val exportedElement: ExportedElement,
    ) : CAdapterCodegenElement

    class Class(
        override val symbol: IrClassSymbol,
        override val exportedElement: ExportedElement,
    ) : CAdapterCodegenElement

    class EnumEntry(
        override val symbol: IrEnumEntrySymbol,
        override val exportedElement: ExportedElement,
    ) : CAdapterCodegenElement
}