/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.backend.konan.ir.KonanSharedVariablesManager
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.SymbolOverIrLookupUtils
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns

class NativePreSerializationLoweringContext(
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
) : PreSerializationLoweringContext(irBuiltIns, configuration) {
    private val konanSymbols = KonanSymbols(
            this, SymbolOverIrLookupUtils(), irBuiltIns, configuration
    )

    override val ir: Ir = KonanIr(konanSymbols)

    override val sharedVariablesManager: SharedVariablesManager = KonanSharedVariablesManager(irBuiltIns, konanSymbols)
}
