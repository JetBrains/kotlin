/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.utilities.KotlinBackendIrHolder
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.lower.ExpectToActualDefaultValueCopier
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.backend.konan.makeEntryPoint
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.backend.konan.objcexport.createTestBundle
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal data class SpecialBackendChecksInput(
        val irModule: IrModuleFragment,
        val symbols: KonanSymbols,
) : KotlinBackendIrHolder {
    override val kotlinIr: IrElement
        get() = irModule
}

internal val SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PsiToIrContext, SpecialBackendChecksInput>(
        "SpecialBackendChecks",
        "Special backend checks",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { context, input ->
    SpecialBackendChecksTraversal(context, input.symbols, input.irModule.irBuiltins).lower(input.irModule)
}

internal val K2SpecialBackendChecksPhase = createSimpleNamedCompilerPhase<PhaseContext, Fir2IrOutput>(
        "SpecialBackendChecks",
        "Special backend checks",
) { context, input ->
    val moduleFragment = input.irModuleFragment
    SpecialBackendChecksTraversal(
            context,
            input.symbols,
            moduleFragment.irBuiltins
    ).lower(moduleFragment)
}

internal val CopyDefaultValuesToActualPhase = createSimpleNamedCompilerPhase<PhaseContext, IrModuleFragment>(
        name = "CopyDefaultValuesToActual",
        description = "Copy default values from expect to actual declarations",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
) { _, input ->
    ExpectToActualDefaultValueCopier(input).process()
}

internal fun <T : PsiToIrContext> PhaseEngine<T>.runSpecialBackendChecks(irModule: IrModuleFragment, symbols: KonanSymbols) {
    runPhase(SpecialBackendChecksPhase, SpecialBackendChecksInput(irModule, symbols))
}

internal fun <T : PhaseContext> PhaseEngine<T>.runK2SpecialBackendChecks(fir2IrOutput: Fir2IrOutput) {
    runPhase(K2SpecialBackendChecksPhase, fir2IrOutput)
}

internal val EntryPointPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment>(
        name = "addEntryPoint",
        description = "Add entry point for program",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
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

internal val CreateTestBundlePhase = createSimpleNamedCompilerPhase<PhaseContext, FrontendPhaseOutput.Full>(
        "CreateTestBundlePhase",
        "Create XCTest bundle"
) { context, input ->
    val config = context.config
    val output = OutputFiles(config.outputPath, config.target, config.produce).mainFile
    createTestBundle(config, input.moduleDescriptor, output)
}

private fun IrModuleFragment.addFile(fileEntry: IrFileEntry, packageFqName: FqName): IrFile {
    val packageFragmentDescriptor = object : PackageFragmentDescriptorImpl(this.descriptor, packageFqName) {
        override fun getMemberScope(): MemberScope = MemberScope.Empty
    }

    return IrFileImpl(fileEntry, packageFragmentDescriptor).also { this.files += it }
}
