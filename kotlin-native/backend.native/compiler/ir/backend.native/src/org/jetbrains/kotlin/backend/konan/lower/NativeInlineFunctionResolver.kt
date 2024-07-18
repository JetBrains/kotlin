/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.utils.addToStdlib.getOrSetIfNull

/**
 * This is the cache of the inline functions that have already been lowered.
 * It is helpful to avoid re-lowering the same function multiple times.
 */
private var IrFunction.loweredInlineFunction: IrFunction? by irAttribute(followAttributeOwner = false)

internal fun IrFunction.getOrSaveLoweredInlineFunction(): IrFunction =
        this::loweredInlineFunction.getOrSetIfNull { this.deepCopyWithSymbols(this.parent) }

// TODO: This is a bit hacky. Think about adopting persistent IR ideas.
internal class NativeInlineFunctionResolver(
        private val generationState: NativeGenerationState,
        override val inlineOnlyPrivateFunctions: Boolean
) : InlineFunctionResolverReplacingCoroutineIntrinsics<Context>(generationState.context) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null

        generationState.inlineFunctionOrigins[function]?.let { return it.irFunction }

        val packageFragment = function.getPackageFragment()
        val moduleDeserializer = context.irLinker.getCachedDeclarationModuleDeserializer(function)
        val irFile: IrFile
        val functionIsCached = moduleDeserializer != null && function.body == null
        val (possiblyLoweredFunction, shouldLower) = if (functionIsCached) {
            // The function is cached, get its body from the IR linker.
            val (firstAccess, deserializedInlineFunction) = moduleDeserializer.deserializeInlineFunction(function)
            generationState.inlineFunctionOrigins[function] = deserializedInlineFunction
            irFile = deserializedInlineFunction.irFile
            function to firstAccess
        } else {
            irFile = packageFragment as IrFile
            val partiallyLoweredFunction = function.loweredInlineFunction
            if (partiallyLoweredFunction == null)
                function to true
            else {
                generationState.inlineFunctionOrigins[function] =
                        InlineFunctionOriginInfo(partiallyLoweredFunction, irFile, function.startOffset, function.endOffset)
                partiallyLoweredFunction to false
            }
        }

        if (shouldLower) {
            lower(possiblyLoweredFunction, irFile, functionIsCached)
            if (!functionIsCached) {
                generationState.inlineFunctionOrigins[function] =
                        InlineFunctionOriginInfo(possiblyLoweredFunction.getOrSaveLoweredInlineFunction(),
                                irFile, function.startOffset, function.endOffset)
            }
        }
        return possiblyLoweredFunction
    }

    private fun lower(function: IrFunction, irFile: IrFile, functionIsCached: Boolean) {
        val body = function.body ?: return

        val experimentalDoubleInlining = context.config.configuration.getBoolean(KlibConfigurationKeys.EXPERIMENTAL_DOUBLE_INLINING)

        TypeOfLowering(context).lower(body, function, irFile)

        NullableFieldsForLateinitCreationLowering(context).lowerWithLocalDeclarations(function)
        NullableFieldsDeclarationLowering(context).lowerWithLocalDeclarations(function)
        LateinitUsageLowering(context).lower(body, function)

        SharedVariablesLowering(context).lower(body, function)

        OuterThisInInlineFunctionsSpecialAccessorLowering(
                context,
                generatePublicAccessors = !experimentalDoubleInlining // Make accessors public if `SyntheticAccessorLowering` is disabled.
        ).lowerWithoutAddingAccessorsToParents(function)

        LocalClassesInInlineLambdasLowering(context).lower(body, function)

        if (!(context.config.produce.isCache || functionIsCached)) {
            // Do not extract local classes off of inline functions from cached libraries.
            LocalClassesInInlineFunctionsLowering(context).lower(body, function)
            LocalClassesExtractionFromInlineFunctionsLowering(context).lower(body, function)
        }

        NativeInlineCallableReferenceToLambdaPhase(generationState).lower(function)
        ArrayConstructorLowering(context).lower(body, function)
        WrapInlineDeclarationsWithReifiedTypeParametersLowering(context).lower(body, function)

        if (experimentalDoubleInlining) {
            NativeIrInliner(generationState, inlineOnlyPrivateFunctions = true).lower(body, function)
            SyntheticAccessorLowering(context).lowerWithoutAddingAccessorsToParents(function)
        }
    }

    private fun DeclarationTransformer.lowerWithLocalDeclarations(function: IrFunction) {
        if (transformFlat(function) != null)
            error("Unexpected transformation of function ${function.dump()}")
    }
}
