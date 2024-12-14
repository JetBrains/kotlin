/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.konan.driver.BasicPhaseContext
import org.jetbrains.kotlin.backend.konan.ir.KonanIr
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl

internal abstract class KonanBackendContext(config: KonanConfig) : BasicPhaseContext(config), CommonBackendContext {
    abstract val builtIns: KonanBuiltIns

    abstract override val ir: KonanIr

    override val mapping: Mapping = Mapping()

    override val irFactory: IrFactory = IrFactoryImpl

    override val messageCollector: MessageCollector
        get() = super<BasicPhaseContext>.messageCollector
}
