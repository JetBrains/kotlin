/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.InlineFunctionOriginInfo
import org.jetbrains.kotlin.backend.konan.NativeMapping
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*

internal class InlineFunctionsSupport(mapping: NativeMapping) {
    private val notLoweredInlineFunctions = mapping.notLoweredInlineFunctions

    fun saveNonLoweredInlineFunction(function: IrFunction) {
        getNonLoweredInlineFunction(function, copy = false)
    }

    fun getNonLoweredInlineFunction(function: IrFunction, copy: Boolean): IrFunction {
        val notLoweredInlineFunction = notLoweredInlineFunctions.getOrPut(function.symbol) {
            function.deepCopyWithVariables().also { it.patchDeclarationParents(function.parent) }
        }
        return if (copy)
            notLoweredInlineFunction.deepCopyWithVariables().also { it.patchDeclarationParents(function.parent) }
        else
            notLoweredInlineFunction
    }
}

// TODO: This is a bit hacky. Think about adopting persistent IR ideas.
internal class NativeInlineFunctionResolver(override val context: Context) : DefaultInlineFunctionResolver(context) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {
        val function = super.getFunctionDeclaration(symbol)

        context.generationState.loweredInlineFunctions[function]?.let { return it.irFunction }

        val packageFragment = function.getPackageFragment()
        val (possiblyLoweredFunction, shouldLower) = if (packageFragment !is IrExternalPackageFragment) {
            context.inlineFunctionsSupport.getNonLoweredInlineFunction(function, copy = context.config.producePerFileCache).also {
                context.generationState.loweredInlineFunctions[function] =
                        InlineFunctionOriginInfo(it, packageFragment as IrFile, function.startOffset, function.endOffset)
            } to true
        } else {
            // The function is from Lazy IR, get its body from the IR linker.
            val moduleDescriptor = packageFragment.packageFragmentDescriptor.containingDeclaration
            val moduleDeserializer = context.irLinker.moduleDeserializers[moduleDescriptor]
                    ?: error("No module deserializer for ${function.render()}")
            require(context.config.cachedLibraries.isLibraryCached(moduleDeserializer.klib)) {
                "No IR and no cache for ${function.render()}"
            }
            val (shouldLower, deserializedInlineFunction) = moduleDeserializer.deserializeInlineFunction(function)
            context.generationState.loweredInlineFunctions[function] = deserializedInlineFunction
            function to shouldLower
        }

        if (!shouldLower) return possiblyLoweredFunction

        val body = possiblyLoweredFunction.body ?: return possiblyLoweredFunction

        PreInlineLowering(context).lower(body, possiblyLoweredFunction, context.generationState.loweredInlineFunctions[function]!!.irFile)

        ArrayConstructorLowering(context).lower(body, possiblyLoweredFunction)

        NullableFieldsForLateinitCreationLowering(context).lowerWithLocalDeclarations(possiblyLoweredFunction)
        NullableFieldsDeclarationLowering(context).lowerWithLocalDeclarations(possiblyLoweredFunction)
        LateinitUsageLowering(context).lower(body, possiblyLoweredFunction)

        SharedVariablesLowering(context).lower(body, possiblyLoweredFunction)

        OuterThisLowering(context).lower(possiblyLoweredFunction)

        LocalClassesInInlineLambdasLowering(context).lower(body, possiblyLoweredFunction)

        if (context.llvmModuleSpecification.containsDeclaration(function)) {
            // Do not extract local classes off of inline functions from cached libraries.
            LocalClassesInInlineFunctionsLowering(context).lower(body, possiblyLoweredFunction)
            LocalClassesExtractionFromInlineFunctionsLowering(context).lower(body, possiblyLoweredFunction)
        }

        WrapInlineDeclarationsWithReifiedTypeParametersLowering(context).lower(body, possiblyLoweredFunction)

        return possiblyLoweredFunction
    }

    private fun DeclarationTransformer.lowerWithLocalDeclarations(function: IrFunction) {
        if (transformFlat(function) != null)
            error("Unexpected transformation of function ${function.dump()}")
    }
}
