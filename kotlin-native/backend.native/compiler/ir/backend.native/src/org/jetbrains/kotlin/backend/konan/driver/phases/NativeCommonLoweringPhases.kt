/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.isReifiable
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.lower.inline.LocalClassesInInlineLambdasLowering
import org.jetbrains.kotlin.backend.common.lower.inline.OuterThisInInlineFunctionsSpecialAccessorLowering
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.jvm.ir.isReifiedTypeParameter
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.config.phaser.NamedCompilerPhase
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.inline.*

private val assertionWrapperPhase = createModuleLoweringPhase(
        lowering = ::NativeAssertionWrapperLowering,
        name = "AssertionWrapperLowering",
)

private val lateinitPhase = createModuleLoweringPhase(
        ::LateinitLowering,
        name = "Lateinit",
)

private val wrapInlineDeclarationsWithReifiedTypeParametersLowering = createModuleLoweringPhase(
        ::WrapInlineDeclarationsWithReifiedTypeParametersLowering,
        name = "WrapInlineDeclarationsWithReifiedTypeParameters",
)

private val sharedVariablesPhase = createModuleLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        prerequisite = setOf(lateinitPhase)
)

private val outerThisSpecialAccessorInInlineFunctionsPhase = createModuleLoweringPhase(
        ::OuterThisInInlineFunctionsSpecialAccessorLowering,
        name = "OuterThisInInlineFunctionsSpecialAccessorLowering",
)

private val extractLocalClassesFromInlineLambdas = createModuleLoweringPhase(
        ::LocalClassesInInlineLambdasLowering,
        name = "ExtractLocalClassesFromInlineLambda",
        prerequisite = setOf(sharedVariablesPhase),
)

private val inlineCallableReferenceToLambdaPhase = createModuleLoweringPhase(
        lowering = { context: LoweringContext ->
            CommonInlineCallableReferenceToLambdaPhase(
                    context,
                    PreSerializationPrivateInlineFunctionResolver(context, allowExternalInlining = false)
            )
        },
        name = "NativeInlineCallableReferenceToLambdaPhase",
)

private val arrayConstructorPhase = createModuleLoweringPhase(
        ::ArrayConstructorLowering,
        name = "ArrayConstructor",
)

/**
 * The first phase of inlining (inline only private functions).
 */
private val inlineOnlyPrivateFunctionsPhase = createModuleLoweringPhase(
        lowering = { context: LoweringContext ->
            FunctionInlining(
                    context,
                    inlineFunctionResolver = PreSerializationPrivateInlineFunctionResolver(context, allowExternalInlining = false),
                    produceOuterThisFields = false,
            )
        },
        name = "InlineOnlyPrivateFunctions",
)

private val syntheticAccessorGenerationPhase = createModuleLoweringPhase(
        lowering = ::SyntheticAccessorLowering,
        name = "SyntheticAccessorGeneration",
        prerequisite = setOf(inlineOnlyPrivateFunctionsPhase),
)

private val validateIrAfterInliningOnlyPrivateFunctionsFirstPhase = createModuleLoweringPhase(
        name = "ValidateIrAfterInliningOnlyPrivateFunctions",
        lowering = { context: LoweringContext ->
            IrValidationAfterInliningOnlyPrivateFunctionsPhase(
                    context = context,
                    checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                        val inlineFunction = inlineFunctionUseSite.symbol.owner
                        when {
                            // TODO: remove this condition after the fix of KT-69457:
                            inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                            // Call sites of non-private functions are allowed at this stage.
                            else -> !inlineFunction.isConsideredAsPrivateForInlining()
                        }
                    }
            )
        }
)

/**
 * The second phase of inlining (inline all functions).
 */
@Suppress("Unused") // KT-72441
private val inlineAllFunctions = createModuleLoweringPhase(
        lowering = { context: LoweringContext ->
            FunctionInlining(
                    context,
                    inlineFunctionResolver = PreSerializationNonPrivateInlineFunctionResolver(context, allowExternalInlining = false, irMangler = KonanManglerIr),
                    produceOuterThisFields = false,
            )
        },
        name = "InlineAllFunctions",
)

@Suppress("Unused") // KT-72441
private val validateIrAfterInliningAllFunctionsFirstPhase = createModuleLoweringPhase(
        name = "ValidateIrAfterInliningAllFunctions",
        lowering = { context: LoweringContext ->
            IrValidationAfterInliningAllFunctionsPhase(
                    context = context,
                    checkInlineFunctionCallSites = { inlineFunctionUseSite ->
                        // No inline function call sites should remain at this stage.
                        val inlineFunction = inlineFunctionUseSite.symbol.owner
                        when {
                            // TODO: remove this condition after the fix of KT-66734:
                            inlineFunction.isExternal -> true // temporarily permitted

                            // TODO: remove this condition after the fix of KT-69457:
                            inlineFunctionUseSite is IrFunctionReference && !inlineFunction.isReifiable() -> true // temporarily permitted

                            // it's fine to have typeOf<T> with reified T, it would be correctly handled by inliner on inlining to next use-sites.
                            // maybe it should be replaced by separate node to avoid this special case and simplify detection code - KT-70360
                            Symbols.isTypeOfIntrinsic(inlineFunction.symbol) && inlineFunctionUseSite.typeArguments[0]?.isReifiedTypeParameter == true -> true

                            else -> false // forbidden
                        }
                    }
            )
        }
)


internal val loweringsOfTheFirstPhase: List<SimpleNamedCompilerPhase<LoweringContext, IrModuleFragment, IrModuleFragment>> = listOf(
        assertionWrapperPhase,
        lateinitPhase,
        sharedVariablesPhase,
        outerThisSpecialAccessorInInlineFunctionsPhase,
        extractLocalClassesFromInlineLambdas,
        inlineCallableReferenceToLambdaPhase,
        arrayConstructorPhase,
        wrapInlineDeclarationsWithReifiedTypeParametersLowering,
        inlineOnlyPrivateFunctionsPhase,
        syntheticAccessorGenerationPhase,
        validateIrAfterInliningOnlyPrivateFunctionsFirstPhase,
//         TODO KT-72441 add public inlining to this list
//        inlineAllFunctions,
//        validateIrAfterInliningAllFunctionsFirstPhase
)

private fun <Ctx : LoweringContext> createModuleLoweringPhase(
        lowering: (Ctx) -> ModuleLoweringPass,
        name: String,
        prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
): SimpleNamedCompilerPhase<Ctx, IrModuleFragment, IrModuleFragment> = createSimpleNamedCompilerPhase(
        name,
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irModule -> irModule },
        op = { context, irModule ->
            lowering(context).lower(irModule)
            irModule
        }
)