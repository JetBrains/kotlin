/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*

internal class InlineFunctionsSupport(mapping: NativeMapping) {
    // Inline functions lowered up to just before the inliner.
    private val partiallyLoweredInlineFunctions = mapping.partiallyLoweredInlineFunctions

    fun savePartiallyLoweredInlineFunction(function: IrFunction) =
            function.deepCopyWithVariables().also {
                it.patchDeclarationParents(function.parent)
                partiallyLoweredInlineFunctions[function.symbol] = it
            }

    fun getPartiallyLoweredInlineFunction(function: IrFunction) =
            partiallyLoweredInlineFunctions[function.symbol]
}

// TODO: This is a bit hacky. Think about adopting persistent IR ideas.
internal class NativeInlineFunctionResolver(override val context: Context, val generationState: NativeGenerationState) : DefaultInlineFunctionResolver(context) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction {
        val function = super.getFunctionDeclaration(symbol)

        generationState.inlineFunctionOrigins[function]?.let { return it.irFunction }

        val packageFragment = function.getPackageFragment()
        val moduleDeserializer = context.irLinker.getCachedDeclarationModuleDeserializer(function)
        val irFile: IrFile
        val (possiblyLoweredFunction, shouldLower) = if (moduleDeserializer != null) {
            // The function is cached, get its body from the IR linker.
            val (firstAccess, deserializedInlineFunction) = moduleDeserializer.deserializeInlineFunction(function)
            generationState.inlineFunctionOrigins[function] = deserializedInlineFunction
            irFile = deserializedInlineFunction.irFile
            function to firstAccess
        } else {
            irFile = packageFragment as IrFile
            val partiallyLoweredFunction = context.inlineFunctionsSupport.getPartiallyLoweredInlineFunction(function)
            if (partiallyLoweredFunction == null)
                function to true
            else {
                generationState.inlineFunctionOrigins[function] =
                        InlineFunctionOriginInfo(partiallyLoweredFunction, irFile, function.startOffset, function.endOffset)
                partiallyLoweredFunction to false
            }
        }

        if (shouldLower) {
            val functionIsCached = moduleDeserializer != null
            lower(possiblyLoweredFunction, irFile, functionIsCached)
            if (!functionIsCached) {
                generationState.inlineFunctionOrigins[function] =
                        InlineFunctionOriginInfo(context.inlineFunctionsSupport.savePartiallyLoweredInlineFunction(possiblyLoweredFunction),
                                irFile, function.startOffset, function.endOffset)
            }
        }
        return possiblyLoweredFunction
    }

    private fun lower(function: IrFunction, irFile: IrFile, functionIsCached: Boolean) {
        val body = function.body ?: return

        PreInlineLowering(context).lower(body, function, irFile)

        ArrayConstructorLowering(context).lower(body, function)

        NullableFieldsForLateinitCreationLowering(context).lowerWithLocalDeclarations(function)
        NullableFieldsDeclarationLowering(context).lowerWithLocalDeclarations(function)
        LateinitUsageLowering(context).lower(body, function)

        SharedVariablesLowering(context).lower(body, function)

        OuterThisLowering(context).lower(function)

        LocalClassesInInlineLambdasLowering(context).lower(body, function)

        if (!(context.config.produce.isCache || functionIsCached)) {
            // Do not extract local classes off of inline functions from cached libraries.
            LocalClassesInInlineFunctionsLowering(context).lower(body, function)
            LocalClassesExtractionFromInlineFunctionsLowering(context).lower(body, function)
        }

        WrapInlineDeclarationsWithReifiedTypeParametersLowering(context).lower(body, function)
    }

    private fun DeclarationTransformer.lowerWithLocalDeclarations(function: IrFunction) {
        if (transformFlat(function) != null)
            error("Unexpected transformation of function ${function.dump()}")
    }
}
