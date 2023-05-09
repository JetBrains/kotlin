/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.fir2Ir
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrPluginContext
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary

internal data class Fir2IrOutput(
        val firResult: FirResult,
        val symbols: KonanSymbols,
        val irModuleFragment: IrModuleFragment,
        val components: Fir2IrComponents,
        val pluginContext: Fir2IrPluginContext,
        val irActualizedResult: IrActualizedResult?,
        val usedLibraries: Set<KotlinResolvedLibrary>
)

internal val Fir2IrPhase = createSimpleNamedCompilerPhase(
        "Fir2Ir", "Compiler Fir2Ir Frontend phase",
        outputIfNotEnabled = { _, _, _, _ -> error("Fir2Ir phase cannot be disabled") }
) { context: PhaseContext, input: FirOutput.Full ->
    context.fir2Ir(input)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runFir2Ir(input: FirOutput.Full): Fir2IrOutput {
    return this.runPhase(Fir2IrPhase, input)
}
