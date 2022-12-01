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
import org.jetbrains.kotlin.backend.konan.llvm.printBitcodePhase
import org.jetbrains.kotlin.backend.konan.llvm.verifyBitcodePhase
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objectFilesPhase
import org.jetbrains.kotlin.backend.konan.shouldDefineFunctionClasses
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal fun createBackendContext(
        config: KonanConfig,
        frontendOutput: FrontendPhaseOutput.Full,
        psiToIrOutput: PsiToIrOutput,
        objCInterface: ObjCExportedInterface? = null,
        objCCodeSpec: ObjCExportCodeSpec? = null
): Context =
        Context(
                config,
                frontendOutput.environment,
                frontendOutput.frontendServices,
                frontendOutput.bindingContext,
                frontendOutput.moduleDescriptor
        ).also {
            it.populateAfterPsiToIr(psiToIrOutput)
            it.objCExportedInterface = objCInterface
            it.objCExportCodeSpec = objCCodeSpec
        }

internal fun PhaseEngine<PhaseContext>.runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
    val frontendOutput = useContext(FrontendContextImpl(config)) { it.runFrontend(environment) }
    return frontendOutput as? FrontendPhaseOutput.Full
}

internal fun PhaseEngine<PhaseContext>.runPsiToIr(
        frontendOutput: FrontendPhaseOutput.Full,
        objCInterface: ObjCExportedInterface?,
        isProducingLibrary: Boolean,
        additionalSteps: (PhaseEngine<out PsiToIrContext>) -> Unit = {}
): Pair<PsiToIrOutput, ObjCExportCodeSpec?> {
    val config = this.context.config
    val psiToIrContext = PsiToIrContextImpl(config, frontendOutput.moduleDescriptor, frontendOutput.bindingContext)
    val (psiToIrOutput, objCCodeSpec) = useContext(psiToIrContext) { psiToIrEngine ->
        additionalSteps(psiToIrEngine)
        val objCCodeSpec = objCInterface?.let { psiToIrEngine.runPhase(CreateObjCExportCodeSpecPhase, it) }
        val output = psiToIrEngine.runPsiToIr(frontendOutput, isProducingLibrary)
        psiToIrEngine.runSpecialBackendChecks(output)
        output to objCCodeSpec
    }
    runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput.irModule)
    return psiToIrOutput to objCCodeSpec
}

internal fun PhaseEngine<Context>.processModuleFragments(
        input: IrModuleFragment,
        action: (NativeGenerationState, IrModuleFragment) -> Unit
): Unit = if (context.config.producePerFileCache) {
    val module = context.irModules[context.config.libraryToCache!!.klib.libraryName]
            ?: error("No module for the library being cached: ${context.config.libraryToCache!!.klib.libraryName}")

    val files = module.files.toList()
    module.files.clear()
    val functionInterfaceFiles = files.filter { it.isFunctionInterfaceFile }

    for (file in files) {
        if (file.isFunctionInterfaceFile) continue

        context.generationState = NativeGenerationState(
                context.config,
                context,
                CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())
        )

        module.files += file
        if (context.generationState.shouldDefineFunctionClasses)
            module.files += functionInterfaceFiles

        action(context.generationState, input)

        module.files.clear()
        context.irModule!!.files.clear() // [dependenciesLowerPhase] puts all files to [context.irModule] for codegen.
    }

    module.files += files
} else {
    context.generationState = NativeGenerationState(context.config, context, context.config.libraryToCache?.strategy)
    action(context.generationState, input)
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
    runBackendCodegen(module)
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

internal fun PhaseEngine<NativeGenerationState>.runBackendCodegen(module: IrModuleFragment) {
    runPhaseInParentContext(allLoweringsPhase, module)
    runPhaseInParentContext(dependenciesLowerPhase, module)
    runPhaseInParentContext(bitcodePhase, module)
    runPhaseInParentContext(verifyBitcodePhase, module)
    runPhaseInParentContext(printBitcodePhase, module)
    runPhaseInParentContext(linkBitcodeDependenciesPhase, module)
    runPhaseInParentContext(bitcodePostprocessingPhase, module)
}
