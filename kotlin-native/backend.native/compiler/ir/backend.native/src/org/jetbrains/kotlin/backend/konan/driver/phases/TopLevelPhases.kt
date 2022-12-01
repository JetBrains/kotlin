/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import llvm.LLVMWriteBitcodeToFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.bitcodePhase
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.runPhaseInParentContext
import org.jetbrains.kotlin.backend.konan.entryPointPhase
import org.jetbrains.kotlin.backend.konan.linkerPhase
import org.jetbrains.kotlin.backend.konan.llvm.linkBitcodeDependenciesPhase
import org.jetbrains.kotlin.backend.konan.llvm.verifyBitcodePhase
import org.jetbrains.kotlin.backend.konan.objectFilesPhase
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrModuleFragmentImpl
import org.jetbrains.kotlin.backend.konan.shouldDefineFunctionClasses
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal fun createBackendContext(config: KonanConfig, frontendOutput: FrontendPhaseOutput.Full, psiToIrOutput: PsiToIrOutput): Context =
        Context(
                config,
                frontendOutput.environment,
                frontendOutput.frontendServices,
                frontendOutput.bindingContext,
                frontendOutput.moduleDescriptor
        ).also {
            it.populateAfterPsiToIr(psiToIrOutput)
        }

internal fun PhaseEngine<PhaseContext>.runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
    val frontendOutput = useContext(FrontendContextImpl(config)) { it.runFrontend(environment) }
    return frontendOutput as? FrontendPhaseOutput.Full
}

internal fun PhaseEngine<PhaseContext>.runPsiToIr(frontendOutput: FrontendPhaseOutput.Full, isProducingLibrary: Boolean): PsiToIrOutput {
    val config = this.context.config
    val psiToIrContext = PsiToIrContextImpl(config, frontendOutput.moduleDescriptor, frontendOutput.bindingContext)
    val psiToIrOutput = useContext(psiToIrContext) { psiToIrEngine ->
        val output = psiToIrEngine.runPsiToIr(frontendOutput, isProducingLibrary)
        psiToIrEngine.runSpecialBackendChecks(output)
        output
    }
    runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput.irModule)
    return psiToIrOutput
}

/**
 * Splits the given [module] according to [Context] configuration
 * and processes each part in [action].
 */
internal fun PhaseEngine<Context>.processModuleFragments(
        module: IrModuleFragment,
): Sequence<Pair<NativeGenerationState, IrModuleFragment>> = if (context.config.producePerFileCache) {
    val files = module.files.toList()
    files.asSequence().filterNot { it.isFunctionInterfaceFile }.map { file ->
        val moduleFragment = KonanIrModuleFragmentImpl(module.descriptor, module.irBuiltins, listOf(file))
        val generationState = NativeGenerationState(context.config, context,
                CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())
        )
        if (generationState.shouldDefineFunctionClasses) {
            moduleFragment.files += files.filter { it.isFunctionInterfaceFile }
        }
        Pair(generationState, moduleFragment)
    }
} else {
    val generationState = NativeGenerationState(context.config, context, context.config.libraryToCache?.strategy)
    generateSequence {
        Pair(generationState, module)
    }
}

/**
 * Performs all the hard work:
 * 1. Runs IR lowerings
 * 2. Runs LTO.
 * 3. Translates IR to LLVM IR.
 * 4. Optimizes it.
 * 5. Serializes it to a bitcode file.
 * 6. Compiles bitcode to an object file.
 * 7. Performs binary linkage.
 * ... And stores additional cache info.
 *
 * TODO: Split into more granular phases with explicit inputs and outputs.
 */
internal fun PhaseEngine<NativeGenerationState>.runLowerAndCompile(module: IrModuleFragment) {
    if (context.config.produce.isCache) {
        runPhaseInParentContext(buildAdditionalCacheInfoPhase)
    }
    if (context.config.produce == CompilerOutputKind.PROGRAM) {
        runPhaseInParentContext(entryPointPhase, module)
    }
    backendCodegen(module)
    if (context.config.produce.isCache) {
        runPhaseInParentContext(saveAdditionalCacheInfoPhase)
    }

    run {
        val output = context.tempFiles.nativeBinaryFileName
        context.bitcodeFileName = output
        // Insert `_main` after pipeline, so we won't worry about optimizations corrupting entry point.
        insertAliasToEntryPoint(context)
        LLVMWriteBitcodeToFile(context.llvm.module, output)
    }
    runPhaseInParentContext(objectFilesPhase)
    runPhaseInParentContext(linkerPhase)
    if (context.config.produce.isCache) {
        runPhaseInParentContext(finalizeCachePhase)
    }
}

internal fun PhaseEngine<NativeGenerationState>.backendCodegen(module: IrModuleFragment) {
    runPhaseInParentContext(allLoweringsPhase, module)
    runPhaseInParentContext(dependenciesLowerPhase, module)
    runPhaseInParentContext(bitcodePhase, module)
    runPhaseInParentContext(verifyBitcodePhase, module)
    runPhaseInParentContext(linkBitcodeDependenciesPhase, module)
    runPhaseInParentContext(bitcodePostprocessingPhase, module)
}
