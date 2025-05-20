/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.CallInlinerStrategy
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.PreSerializationLoweringContext
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.KonanSharedVariablesManager
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.inline.InlineFunctionResolverReplacingCoroutineIntrinsics
import org.jetbrains.kotlin.ir.inline.InlineMode
import org.jetbrains.kotlin.ir.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.ir.inline.SyntheticAccessorLowering
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.file

private var IrFunction.wasLowered: Boolean? by irAttribute(copyByDefault = true)

internal class NativeInlineFunctionResolver(
        val generationState: NativeGenerationState,
        inlineMode: InlineMode,
) : InlineFunctionResolverReplacingCoroutineIntrinsics<Context>(
        context = generationState.context,
        inlineMode = inlineMode,
        callInlinerStrategy = NativeCallInlinerStrategy(generationState.context, generationState.context.symbols)
) {
    override fun getFunctionDeclaration(symbol: IrFunctionSymbol): IrFunction? {
        val function = super.getFunctionDeclaration(symbol) ?: return null

        if (function.body != null) {
            // TODO this `if` check can be dropped after KT-72441
            if (function.wasLowered != true) {
                lower(function)
                function.wasLowered = true
            }
            return function
        }

        context.getInlineFunctionDeserializer(function).deserializeInlineFunction(function)
        lower(function)
        function.wasLowered = true

        return function
    }

    private fun lower(function: IrFunction) {
        val body = function.body ?: return

        UpgradeCallableReferences(context).lower(function)

        NativeAssertionWrapperLowering(context).lower(function)

        LateinitLowering(context).lower(body)

        SharedVariablesLowering(context).lower(body, function)

        LocalClassesInInlineLambdasLowering(context).lower(body, function)

        ArrayConstructorLowering(context).lower(body, function)

        NativeIrInliner(generationState, inlineMode = InlineMode.PRIVATE_INLINE_FUNCTIONS).lower(body, function)
        OuterThisInInlineFunctionsSpecialAccessorLowering(context).lowerWithoutAddingAccessorsToParents(function)
        SyntheticAccessorLowering(context).lowerWithoutAddingAccessorsToParents(function)
    }

    class NativeCallInlinerStrategy(val context: LoweringContext, val symbols: KonanSymbols) : CallInlinerStrategy {
        private lateinit var builder: NativeRuntimeReflectionIrBuilder
        override fun at(container: IrDeclaration, expression: IrExpression) {
            builder = symbols.irBuiltIns.createIrBuilder(container.symbol, expression.startOffset, expression.endOffset)
                    .toNativeRuntimeReflectionBuilder(symbols) { message ->
                        this@NativeCallInlinerStrategy.context.reportCompilationError(message, expression.getCompilerMessageLocation(container.file))
                    }
        }

        override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
            return builder.irKType(nonSubstitutedTypeArgument)
        }
    }
}

class NativePreSerializationLoweringContext(
        irBuiltIns: IrBuiltIns,
        configuration: CompilerConfiguration,
        diagnosticReporter: DiagnosticReporter,
) : PreSerializationLoweringContext(irBuiltIns, configuration, diagnosticReporter) {
    private val konanSymbols = KonanSymbols(this, irBuiltIns, configuration)

    override val symbols: Symbols = konanSymbols

    override val sharedVariablesManager: SharedVariablesManager = KonanSharedVariablesManager(irBuiltIns, konanSymbols)

    override val callInlinerStrategy: CallInlinerStrategy
        get() = NativeInlineFunctionResolver.NativeCallInlinerStrategy(this, konanSymbols)
}
