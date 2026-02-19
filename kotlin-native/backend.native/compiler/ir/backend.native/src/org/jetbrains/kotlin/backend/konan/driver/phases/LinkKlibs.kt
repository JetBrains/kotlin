/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.phaser.createSimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.backend.konan.KonanReflectionTypes
import org.jetbrains.kotlin.backend.konan.LinkKlibsContext
import org.jetbrains.kotlin.backend.konan.LinkKlibsInput
import org.jetbrains.kotlin.backend.konan.LinkKlibsOutput
import org.jetbrains.kotlin.backend.konan.driver.BasicNativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.linkKlibs
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext

internal class LinkKlibsContextImpl(
        config: NativeSecondStageCompilationConfig,
        private val moduleDescriptor: ModuleDescriptor,
        override val bindingContext: BindingContext,
) : BasicNativeBackendPhaseContext(config), LinkKlibsContext {
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

internal val LinkKlibsPhase = createSimpleNamedCompilerPhase<LinkKlibsContext, LinkKlibsInput, LinkKlibsOutput>(
        "LinkKlibs",
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ -> error("LinkKlibs phase cannot be disabled") }
) { context, input ->
    context.linkKlibs(input)
}
