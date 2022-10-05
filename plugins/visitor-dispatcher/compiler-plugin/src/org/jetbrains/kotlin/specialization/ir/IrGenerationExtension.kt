/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class SpecializationIrGenerationExtension: IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val functionCollector = MonomorphicFunctionCollector()
        val callCollector = MonomorphicCallsCollector()
        moduleFragment.acceptVoid(functionCollector)
        moduleFragment.acceptVoid(callCollector)

        val specializer = MonomorphicFunctionSpecializer(moduleFragment)
        for (f in functionCollector.functions) {
            specializer.registerFunction(f)
        }

        specializer.generateAllMonomorphicSpecializations()

        val replacer = MonomorphicCallsReplacer()
        for (call in callCollector.calls) {
            val specialization = specializer.callRefinementProvider.refine(call)
            if (specialization != null) {
                replacer.addReplacement(call, specialization)
            }
        }
        replacer.replaceInPlace(moduleFragment)
    }
}
