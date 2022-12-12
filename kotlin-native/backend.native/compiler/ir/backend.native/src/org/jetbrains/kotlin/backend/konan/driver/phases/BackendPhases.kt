/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.backend.konan.makeEntryPoint
import org.jetbrains.kotlin.name.FqName

internal val SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PsiToIrContext, PsiToIrOutput>(
        "SpecialBackendChecks",
        "Special backend checks",
) { context, input ->
    SpecialBackendChecksTraversal(context, context.interopBuiltIns, input.symbols, input.irModule.irBuiltins).lower(input.irModule)
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


internal val EntryPointPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "addEntryPoint",
        description = "Add entry point for program"
) { context, module ->
    val parent = context.context
    val entryPoint = parent.ir.symbols.entryPoint!!.owner
    val file: IrFile = if (context.llvmModuleSpecification.containsDeclaration(entryPoint)) {
        entryPoint.file
    } else {
        // `main` function is compiled to other LLVM module.
        // For example, test running support uses `main` defined in stdlib.
        module.addFile(NaiveSourceBasedFileEntryImpl("entryPointOwner"), FqName("kotlin.native.internal.abi"))
    }

    file.addChild(makeEntryPoint(context))
}
