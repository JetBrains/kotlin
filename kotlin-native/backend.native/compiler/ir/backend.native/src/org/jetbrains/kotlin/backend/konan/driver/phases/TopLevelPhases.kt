/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.driver.runPhaseInParentContext
import org.jetbrains.kotlin.backend.konan.llvm.linkBitcodeDependenciesPhase
import org.jetbrains.kotlin.backend.konan.llvm.printBitcodePhase
import org.jetbrains.kotlin.backend.konan.llvm.verifyBitcodePhase
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

internal fun PhaseEngine<PhaseContext>.runFrontend(config: KonanConfig, environment: KotlinCoreEnvironment): FrontendPhaseOutput.Full? {
    val frontendOutput = useContext(FrontendContextImpl(config)) { it.runFrontend(environment) }
    return frontendOutput as? FrontendPhaseOutput.Full
}

internal fun PhaseEngine<PhaseContext>.runPsiToIr(
        frontendOutput: FrontendPhaseOutput.Full,
        isProducingLibrary: Boolean,
): PsiToIrOutput = runPsiToIr(frontendOutput, isProducingLibrary, {}).first

internal fun <T> PhaseEngine<PhaseContext>.runPsiToIr(
        frontendOutput: FrontendPhaseOutput.Full,
        isProducingLibrary: Boolean,
        produceAdditionalOutput: (PhaseEngine<out PsiToIrContext>) -> T
): Pair<PsiToIrOutput, T> {
    val config = this.context.config
    val psiToIrContext = PsiToIrContextImpl(config, frontendOutput.moduleDescriptor, frontendOutput.bindingContext)
    val (psiToIrOutput, additionalOutput) = useContext(psiToIrContext) { psiToIrEngine ->
        val additionalOutput = produceAdditionalOutput(psiToIrEngine)
        val psiToIrInput = PsiToIrInput(frontendOutput.moduleDescriptor, frontendOutput.environment, isProducingLibrary)
        val output = psiToIrEngine.runPhase(PsiToIrPhase, psiToIrInput)
        psiToIrEngine.runSpecialBackendChecks(output)
        output to additionalOutput
    }
    runPhase(CopyDefaultValuesToActualPhase, psiToIrOutput.irModule)
    return psiToIrOutput to additionalOutput
}

internal fun <C : PhaseContext> PhaseEngine<C>.runBackend(backendContext: Context) {
    useContext(backendContext) { backendEngine ->
        backendEngine.runPhase(functionsWithoutBoundCheck)
        backendEngine.processModuleFragments(backendEngine.context.irModule!!) { generationState, fragment ->
            backendEngine.useContext(generationState) { generationStateEngine ->
                // TODO: We can run compile part in parallel if we get rid of context.generationState.
                generationStateEngine.runLowerAndCompile(fragment)
            }
        }
    }
}

private fun isReferencedByNativeRuntime(declarations: List<IrDeclaration>): Boolean =
        declarations.any {
            it.hasAnnotation(RuntimeNames.exportTypeInfoAnnotation)
                    || it.hasAnnotation(RuntimeNames.exportForCppRuntime)
        } || declarations.any {
            it is IrClass && isReferencedByNativeRuntime(it.declarations)
        }

internal fun PhaseEngine<out Context>.processModuleFragments(
        input: IrModuleFragment,
        action: (NativeGenerationState, IrModuleFragment) -> Unit
): Unit = if (context.config.producePerFileCache) {
    val module = context.irModules[context.config.libraryToCache!!.klib.libraryName]
            ?: error("No module for the library being cached: ${context.config.libraryToCache!!.klib.libraryName}")

    val files = module.files.toList()
    module.files.clear()

    val stdlibIsBeingCached = module.descriptor == context.stdlibModule
    val functionInterfaceFiles = files.takeIf { stdlibIsBeingCached }
            ?.filter { it.isFunctionInterfaceFile }.orEmpty()
    val filesReferencedByNativeRuntime = files.takeIf { stdlibIsBeingCached }
            ?.filter { isReferencedByNativeRuntime(it.declarations) }.orEmpty()

    for (file in files) {
        if (file.isFunctionInterfaceFile) continue

        val generationState = NativeGenerationState(
                context.config,
                context,
                CacheDeserializationStrategy.SingleFile(file.path, file.fqName.asString())
        )
        context.generationState = generationState

        module.files += file
        if (generationState.shouldDefineFunctionClasses)
            module.files += functionInterfaceFiles

        if (generationState.shouldLinkRuntimeNativeLibraries) {
            filesReferencedByNativeRuntime.forEach {
                generationState.dependenciesTracker.add(it)
            }
        }

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
    runPhase(WriteBitcodeFilePhase)
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