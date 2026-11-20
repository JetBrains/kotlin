/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.backend.konan.objcexport.createTestBundle
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal data class SpecialBackendChecksInput(
        val irModule: IrModuleFragment,
        val irBuiltIns: IrBuiltIns,
        val symbols: BackendNativeSymbols,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, SpecialBackendChecksInput>(
        "SpecialBackendChecks",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, input ->
    SpecialBackendChecksTraversal(context, input.symbols, input.irBuiltIns).lower(input.irModule)
}

internal fun <T : NativeBackendPhaseContext> PhaseEngine<T>.runSpecialBackendChecks(irModule: IrModuleFragment, irBuiltIns: IrBuiltIns, symbols: BackendNativeSymbols) {
    runPhase(SpecialBackendChecksPhase, SpecialBackendChecksInput(irModule, irBuiltIns, symbols))
}

internal val CreateTestBundlePhase = createSimpleNamedCompilerPhase<NativeBackendPhaseContext, ModuleDescriptor>(
        "CreateTestBundlePhase",
) { context, moduleDescriptor ->
    val config = context.config
    val output = OutputFiles(config.outputPath, config.target, config.produce).mainFile
    createTestBundle(config, moduleDescriptor, output)
}
