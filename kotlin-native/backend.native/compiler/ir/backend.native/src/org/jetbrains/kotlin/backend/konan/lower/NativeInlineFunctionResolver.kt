/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.DefaultInlineFunctionResolver
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.InlineFunctionOriginInfo
import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render

// TODO: This is a bit hacky. Think about adopting persistent IR ideas.
internal class NativeInlineFunctionResolver(override val context: Context) : DefaultInlineFunctionResolver(context) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {
        val function = super.getFunctionDeclaration(symbol)

        context.specialDeclarationsFactory.loweredInlineFunctions[function]?.let { return it.irFunction }

        val packageFragment = function.findPackage()
        val notLoweredFunction = if (packageFragment !is IrExternalPackageFragment) {
            context.specialDeclarationsFactory.getNonLoweredInlineFunction(function).also {
                context.specialDeclarationsFactory.loweredInlineFunctions[function] =
                        InlineFunctionOriginInfo(it, function.file, function.startOffset, function.endOffset)
            }
        } else {
            // The function is from Lazy IR, get its body from the IR linker.
            val moduleDescriptor = packageFragment.packageFragmentDescriptor.containingDeclaration
            val moduleDeserializer = context.irLinker.cachedLibraryModuleDeserializers[moduleDescriptor]
                    ?: error("No module deserializer for ${function.render()}")
            context.specialDeclarationsFactory.loweredInlineFunctions[function] = moduleDeserializer.deserializeInlineFunction(function)
            function
        }

        val body = notLoweredFunction.body ?: return notLoweredFunction

        PreInlineLowering(context).lower(body, notLoweredFunction, context.specialDeclarationsFactory.loweredInlineFunctions[function]!!.irFile)

        ArrayConstructorLowering(context).lower(body, notLoweredFunction)

        NullableFieldsForLateinitCreationLowering(context).lowerWithLocalDeclarations(notLoweredFunction)
        NullableFieldsDeclarationLowering(context).lowerWithLocalDeclarations(notLoweredFunction)
        LateinitUsageLowering(context).lower(body, notLoweredFunction)

        SharedVariablesLowering(context).lower(body, notLoweredFunction)

        OuterThisLowering(context).lower(notLoweredFunction)

        LocalClassesInInlineLambdasLowering(context).lower(body, notLoweredFunction)

        if (context.llvmModuleSpecification.containsPackageFragment(packageFragment)) {
            // Do not extract local classes off of inline functions from cached libraries.
            LocalClassesInInlineFunctionsLowering(context).lower(body, notLoweredFunction)
            LocalClassesExtractionFromInlineFunctionsLowering(context).lower(body, notLoweredFunction)
        }

        return notLoweredFunction
    }

    private fun DeclarationTransformer.lowerWithLocalDeclarations(function: IrFunction) {
        if (transformFlat(function) != null)
            error("Unexpected transformation of function ${function.dump()}")
    }
}
