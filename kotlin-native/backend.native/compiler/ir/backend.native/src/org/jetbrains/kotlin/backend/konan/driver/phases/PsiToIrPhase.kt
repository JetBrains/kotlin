/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.psiToIr
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

data class PsiToIrInput(
        val frontendPhaseResult: FrontendPhaseResult.Full,
        val isProducingLibrary: Boolean,
)

internal class PsiToIrResult(
        val irModules: Map<String, IrModuleFragment>,
        val irModule: IrModuleFragment,
        val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
        val symbols: KonanSymbols,
        val irLinker: KonanIrLinker?,
)

// TODO: Consider component-based approach
internal interface PsiToIrContext :
        PhaseContext {
    val symbolTable: SymbolTable

    val reflectionTypes: KonanReflectionTypes

    val builtIns: KonanBuiltIns

    val llvmModuleSpecification: LlvmModuleSpecification

    val interopBuiltIns: InteropBuiltIns

    val stdlibModule: ModuleDescriptor
        get() = this.builtIns.any.module
}

internal class PsiToContextImpl(
        config: KonanConfig,
        private val moduleDescriptor: ModuleDescriptor,
        override val symbolTable: SymbolTable
) : BasicPhaseContext(config), PsiToIrContext {
    override val reflectionTypes: KonanReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor)
    }

    override val builtIns: KonanBuiltIns by lazy(LazyThreadSafetyMode.PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    override val llvmModuleSpecification: LlvmModuleSpecification by lazy {
        when {
            config.produce.isCache ->
                CacheLlvmModuleSpecification(this, config.cachedLibraries, config.libraryToCache!!)
            else -> DefaultLlvmModuleSpecification(config.cachedLibraries)
        }
    }

    override fun dispose() {
        // TODO: dispose symbol table
    }
}

internal val PsiToIrPhase = object : SimpleNamedCompilerPhase<PsiToIrContext, PsiToIrInput, PsiToIrResult>(
        "PsiToIr", "Translate PSI to IR",
) {
    override fun phaseBody(context: PsiToIrContext, input: PsiToIrInput): PsiToIrResult {
        return context.psiToIr(input, useLinkerWhenProducingLibrary = false)
    }

    override fun outputIfNotEnabled(context: PsiToIrContext, input: PsiToIrInput): PsiToIrResult {
        error("disabled")
    }
}

internal fun <T: PsiToIrContext> PhaseEngine<T>.runPsiToIr(
        frontendResult: FrontendPhaseResult.Full,
        isProducingLibrary: Boolean
): PsiToIrResult {
    val psiToIrInput = PsiToIrInput(frontendResult, isProducingLibrary)
    val result = this.runPhase(context, PsiToIrPhase, psiToIrInput)
    val originalBindingContext = frontendResult.bindingContext as? CleanableBindingContext
            ?: error("BindingContext should be cleanable in K/N IR to avoid leaking memory: ${frontendResult.bindingContext}")
    originalBindingContext.clear()
    return result
}