/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.KlibSharedVariablesManager
import org.jetbrains.kotlin.backend.konan.driver.BasicNativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl

internal abstract class KonanBackendContext(config: NativeSecondStageCompilationConfig) : BasicNativeBackendPhaseContext(config), CommonBackendContext {
    abstract val builtIns: KonanBuiltIns

    abstract override val symbols: BackendNativeSymbols

    override val sharedVariablesManager by lazy {
        // Creating lazily because builtIns module seems to be incomplete during `link` test;
        // TODO: investigate this.
        KlibSharedVariablesManager(symbols)
    }

    override val irFactory: IrFactory = IrFactoryImpl

    override val messageCollector: MessageCollector
        get() = super<BasicNativeBackendPhaseContext>.messageCollector
}
