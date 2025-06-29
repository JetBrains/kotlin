/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.IrLinkerContext
import org.jetbrains.kotlin.backend.konan.IrLinkerInput
import org.jetbrains.kotlin.backend.konan.IrLinkerOutput
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanReflectionTypes
import org.jetbrains.kotlin.backend.konan.driver.BackendPhaseContext
import org.jetbrains.kotlin.backend.common.phaser.BasicPhaseContext
import org.jetbrains.kotlin.backend.common.phaser.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.linkIrLibraries
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext

internal class IrLinkerContextImpl(
        override val config: KonanConfig,
        private val moduleDescriptor: ModuleDescriptor,
        override val bindingContext: BindingContext,
) : BasicPhaseContext(config.configuration), IrLinkerContext, BackendPhaseContext {

    // TODO: Invalidate properly in dispose method.
    override val symbolTable = SymbolTable(KonanIdSignaturer(KonanManglerDesc), IrFactoryImpl)

    override val reflectionTypes: KonanReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KonanReflectionTypes(moduleDescriptor)
    }

    override val builtIns: KonanBuiltIns by lazy(LazyThreadSafetyMode.PUBLICATION) {
        moduleDescriptor.builtIns as KonanBuiltIns
    }

    override fun dispose() {
        val originalBindingContext = bindingContext as? CleanableBindingContext
                ?: error("BindingContext should be cleanable in K/N IR to avoid leaking memory: $bindingContext")
        originalBindingContext.clear()
    }
}

internal val IrLinkerPhase = createSimpleNamedCompilerPhase<IrLinkerContext, IrLinkerInput, IrLinkerOutput>(
        "IrLinker",
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ -> error("PsiToIr phase cannot be disabled") }
) { context, input ->
    context.linkIrLibraries(input)
}
