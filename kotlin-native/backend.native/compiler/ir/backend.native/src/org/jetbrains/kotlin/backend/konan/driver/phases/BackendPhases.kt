/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal val SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PsiToIrContext, PsiToIrOutput>(
        "SpecialBackendChecks",
        "Special backend checks",
) { context, input ->
    SpecialBackendChecksTraversal(context, context.interopBuiltIns, input.symbols, input.irModule.irBuiltins).lower(input.irModule)
}

internal val K2SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PhaseContext, Fir2IrOutput>(
        "SpecialBackendChecks",
        "Special backend checks",
) { context, input ->
    val moduleFragment = input.fir2irResult.irModuleFragment
    SpecialBackendChecksTraversal(
            context,
            InteropBuiltIns(input.fir2irResult.pluginContext.moduleDescriptor.builtIns as KonanBuiltIns),
            input.symbols,
            moduleFragment.irBuiltins
    ).lower(moduleFragment)
}

internal val CopyDefaultValuesToActualPhase = createSimpleNamedCompilerPhase<PhaseContext, IrModuleFragment>(
        name = "CopyDefaultValuesToActual",
        description = "Copy default values from expect to actual declarations",
) { _, input ->
        ExpectToActualDefaultValueCopier(input).process()
}

internal fun <T : PsiToIrContext> PhaseEngine<T>.runSpecialBackendChecks(psiToIrOutput: PsiToIrOutput) {
    runPhase(SpecialBackendChecksPhase, psiToIrOutput)
}

internal fun <T : PhaseContext> PhaseEngine<T>.runK2SpecialBackendChecks(fir2IrOutput: Fir2IrOutput) {
    runPhase(K2SpecialBackendChecksPhase, fir2IrOutput)
}