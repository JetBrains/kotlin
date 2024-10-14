/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesExtractionFromInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineFunctionsLowering
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.inline.CallInlinerStrategy
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
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
        inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<Context>(generationState.context, inlineMode) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null

        function.loweredInlineFunction?.let { return it }

        val moduleDeserializer = if (function.body == null) context.irLinker.getCachedDeclarationModuleDeserializer(function) else null
        val functionIsCached = moduleDeserializer != null
        if (functionIsCached) {
            // The function is cached, get its body from the IR linker.
            moduleDeserializer.deserializeInlineFunction(function)
        }

        lower(function, functionIsCached)

        return function.getOrSaveLoweredInlineFunction()
    }

    private fun lower(function: IrFunction, functionIsCached: Boolean) {
        val body = function.body ?: return

        val doubleInliningEnabled = !context.config.configuration.getBoolean(KlibConfigurationKeys.NO_DOUBLE_INLINING)

        NativeAssertionWrapperLowering(context).lower(function)

        NullableFieldsForLateinitCreationLowering(context).lowerWithLocalDeclarations(function)
        NullableFieldsDeclarationLowering(context).lowerWithLocalDeclarations(function)
        LateinitUsageLowering(context).lower(body, function)

        SharedVariablesLowering(context).lower(body, function)

        OuterThisInInlineFunctionsSpecialAccessorLowering(
                context,
                generatePublicAccessors = !doubleInliningEnabled // Make accessors public if `SyntheticAccessorLowering` is disabled.
        ).lowerWithoutAddingAccessorsToParents(function)

        LocalClassesInInlineLambdasLowering(context).lower(body, function)

        if (!context.config.produce.isCache && !functionIsCached && !doubleInliningEnabled) {
            // Do not extract local classes off of inline functions from cached libraries.
            LocalClassesInInlineFunctionsLowering(context).lower(body, function)
            LocalClassesExtractionFromInlineFunctionsLowering(context).lower(body, function)
        }

        NativeInlineCallableReferenceToLambdaPhase(generationState).lower(function)
        ArrayConstructorLowering(context).lower(body, function)
        WrapInlineDeclarationsWithReifiedTypeParametersLowering(context).lower(body, function)

        if (doubleInliningEnabled) {
            NativeIrInliner(generationState, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS).lower(body, function)
            SyntheticAccessorLowering(context).lowerWithoutAddingAccessorsToParents(function)
        }
    }

    private fun DeclarationTransformer.lowerWithLocalDeclarations(function: IrFunction) {
        if (transformFlat(function) != null)
            error("Unexpected transformation of function ${function.dump()}")
    }

    override val callInlinerStrategy: CallInlinerStrategy = NativeCallInlinerStrategy()

    inner class NativeCallInlinerStrategy : CallInlinerStrategy {
        private lateinit var builder: NativeRuntimeReflectionIrBuilder
        override fun at(scope: Scope, expression: IrExpression) {
            val symbols = this@NativeInlineFunctionResolver.context.ir.symbols
            builder = context.createIrBuilder(scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                    .toNativeRuntimeReflectionBuilder(symbols) { message ->
                        this@NativeInlineFunctionResolver.context.reportCompilationError(message, getCompilerMessageLocation())
                    }
        }
        override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
            return builder.irKType(nonSubstitutedTypeArgument)
        }
    }
}
