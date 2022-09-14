/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.dispatcher.ir.generators.DispatchFunctionBodyGenerator
import org.jetbrains.kotlin.dispatcher.ir.generators.GetKindFunctionBodyGenerator
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class BodyGenerationIrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val dispatcherContext = DispatcherPluginContext()

        moduleFragment.acceptVoid(GetKindFunctionBodyGenerator(dispatcherContext, pluginContext))
        moduleFragment.acceptVoid(DispatchFunctionBodyGenerator(dispatcherContext, pluginContext))
    }
}

