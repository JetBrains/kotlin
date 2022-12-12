/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.common.lower.ArrayConstructorLowering
import org.jetbrains.kotlin.backend.common.lower.StripTypeAliasDeclarationsLowering
import org.jetbrains.kotlin.backend.common.phaser.AbstractNamedCompilerPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.PhaseEngine
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.backend.common.lower.inline.*
import org.jetbrains.kotlin.backend.common.phaser.SameTypeNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.changePhaserStateType
import org.jetbrains.kotlin.backend.common.lower.coroutines.AddContinuationToNonLocalSuspendFunctionsLowering

internal val ReturnsInsertionPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        //prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase), TODO: if there are no files in the module, this requirement fails.
        op = { context, irFile -> ReturnsInsertionLowering(context.context).lower(irFile) }
)

internal val InlineClassPropertyAccessorsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "InlineClassPropertyAccessorsLowering",
        description = "Inline class property accessors",
        op = { context, irFile -> InlineClassPropertyAccessorsLowering(context.context).lower(irFile) }
)

internal val RedundantCoercionsCleaningPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "RedundantCoercionsCleaning",
        description = "Redundant coercions cleaning",
        op = { context, irFile -> RedundantCoercionsCleaner(context.context).lower(irFile) }
)

internal val RemoveExpectDeclarationsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing",
        op = { _, file -> ExpectDeclarationsRemoving().lower(file) }
)

internal val StripTypeAliasDeclarationsPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "StripTypeAliasDeclarations",
        description = "Strip typealias declarations",
        op = { _, file -> StripTypeAliasDeclarationsLowering().lower(file) },
)

internal val LowerBeforeInlinePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining",
        op = { context, irFile -> PreInlineLowering(context.context).lower(irFile) }
)

internal val ArrayConstructorPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> ArrayConstructorLowering(context.context).lower(irFile) },
        name = "ArrayConstructor",
        description = "Transform `Array(size) { index -> value }` into a loop"
)

internal val LateinitPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile ->
            NullableFieldsForLateinitCreationLowering(context.context).lower(irFile)
            NullableFieldsDeclarationLowering(context.context).lower(irFile)
            LateinitUsageLowering(context.context).lower(irFile)
        },
        name = "Lateinit",
        description = "Lateinit properties lowering"
)

internal val SharedVariablesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        name = "SharedVariables",
        description = "Transform shared variables",
        op = { context, file -> SharedVariablesLowering(context.context).lower(file) }
)

internal val InventNamesForLocalClasses = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, file -> NativeInventNamesForLocalClasses(context).lower(file) },
        name = "InventNamesForLocalClasses",
        description = "Invent names for local classes and anonymous objects"
)

internal val ExtractLocalClassesFromInlineBodies = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile ->
            irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    if (declaration.isInline)
                        context.context.inlineFunctionsSupport.saveNonLoweredInlineFunction(declaration)
                    declaration.acceptChildrenVoid(this)
                }
            })

            LocalClassesInInlineLambdasLowering(context.context).lower(irFile)
            LocalClassesInInlineFunctionsLowering(context.context).lower(irFile)
            LocalClassesExtractionFromInlineFunctionsLowering(context.context).lower(irFile)
        },
        name = "ExtractLocalClassesFromInlineBodies",
        description = "Extraction of local classes from inline bodies",
)

internal val WrapInlineDeclarationsWithReifiedTypeParametersLowering = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> WrapInlineDeclarationsWithReifiedTypeParametersLowering(context.context).lower(irFile) },
        name = "WrapInlineDeclarationsWithReifiedTypeParameters",
        description = "Wrap inline declarations with reified type parameters"
)

internal val InlinePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile ->
            FunctionInlining(context.context, NativeInlineFunctionResolver(context.context, context)).lower(irFile)
        },
        name = "Inline",
        description = "Functions inlining",
//        prerequisite = setOf(lowerBeforeInlinePhase, arrayConstructorPhase, extractLocalClassesFromInlineBodies)
)

// TODO make all lambda-related stuff work with IrFunctionExpression and drop this phase (see kotlin: dd3f8ecaacd)
internal val ProvisionalFunctionExpressionPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { _, irFile -> ProvisionalFunctionExpressionLowering().lower(irFile) },
        name = "FunctionExpression",
        description = "Transform IrFunctionExpression to a local function reference"
)

internal val PostInlinePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> PostInlineLowering(context.context).lower(irFile) },
        name = "PostInline",
        description = "Post-processing after inlining"
)

internal val ContractsDslRemovePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> ContractsDslRemover(context.context).lower(irFile) },
        name = "RemoveContractsDsl",
        description = "Contracts dsl removing"
)

internal val AnnotationImplementationPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> AnnotationImplementationLowering { NativeAnnotationImplementationTransformer(context.context, it) }.lower(irFile) },
        name = "AnnotationImplementation",
        description = "Create synthetic annotations implementations and use them in annotations constructor calls"
)

private fun PhaseEngine<NativeGenerationState>.wrapPhase(
        phase: SameTypeNamedCompilerPhase<Context, IrFile>
): SimpleNamedCompilerPhase<NativeGenerationState, IrFile, Unit> {
    return createSimpleNamedCompilerPhase(
            phase.name,
            phase.description,
            op = { context, irFile -> phase.phaseBody(phaseConfig, phaserState.changePhaserStateType(), context.context, irFile) }
    )
}

internal val UseInternalAbiPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, file -> ImportCachesAbiTransformer(context).lower(file) },
        name = "UseInternalAbi",
        description = "Use internal ABI functions to access private entities"
)

internal val CoroutinesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile ->
            NativeSuspendFunctionsLowering(context).lower(irFile)
            AddContinuationToNonLocalSuspendFunctionsLowering(context.context).lower(irFile)
            NativeAddContinuationToFunctionCallsLowering(context.context).lower(irFile)
            AddFunctionSupertypeToSuspendFunctionLowering(context.context).lower(irFile)
        },
        name = "Coroutines",
        description = "Coroutines lowering",
//        prerequisite = setOf(localFunctionsPhase, finallyBlocksPhase, kotlinNothingValueExceptionPhase)
)

internal val DelegationPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, file -> PropertyDelegationLowering(context).lower(file) },
        name = "Delegation",
        description = "Delegation lowering"
)

internal val FunctionReferencePhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, file -> FunctionReferenceLowering(context).lower(file) },
        name = "FunctionReference",
        description = "Function references lowering",
//        prerequisite = setOf(delegationPhase, localFunctionsPhase) // TODO: make weak dependency on `testProcessorPhase`
)

internal val InteropPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, file -> InteropLowering(context).lower(file) },
        name = "Interop",
        description = "Interop lowering",
//        prerequisite = setOf(inlinePhase, localFunctionsPhase, functionReferencePhase)
)

internal val ObjectClassesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> ObjectClassLowering(context).lower(irFile) },
        name = "ObjectClasses",
        description = "Object classes lowering"
)

internal val BuiltinOperatorPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> BuiltinOperatorLowering(context.context, irFile.module.irBuiltins).lower(irFile) },
        name = "BuiltinOperators",
        description = "BuiltIn operators lowering",
//        prerequisite = setOf(defaultParameterExtentPhase, singleAbstractMethodPhase, enumWhenPhase)
)

internal val DataClassesPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrFile>(
        op = { context, irFile -> DataClassOperatorsLowering(context.context, irFile.module.irBuiltins).lower(irFile) },
        name = "DataClasses",
        description = "Data classes lowering"
)

internal fun PhaseEngine<NativeGenerationState>.runAllLowerings(irModuleFragment: IrModuleFragment) {

    // TODO: Idea: Instead of explicit dependency of phase X, mark Ir with a bit "has state S(X)".
    // TODO: We can use something weaker that NativeGenerationState for Ir Lowerings. BackendContext?
    val allLowerings = listOf<AbstractNamedCompilerPhase<NativeGenerationState, IrFile, Unit>>(
            RemoveExpectDeclarationsPhase,
            StripTypeAliasDeclarationsPhase,
            LowerBeforeInlinePhase,
            ArrayConstructorPhase,
            LateinitPhase,
            SharedVariablesPhase,
            InventNamesForLocalClasses,
            ExtractLocalClassesFromInlineBodies,
            WrapInlineDeclarationsWithReifiedTypeParametersLowering,
            InlinePhase,
            ProvisionalFunctionExpressionPhase,
            PostInlinePhase,
            ContractsDslRemovePhase,
            AnnotationImplementationPhase,
            wrapPhase(rangeContainsLoweringPhase),
            wrapPhase(forLoopsPhase),
            wrapPhase(flattenStringConcatenationPhase),
            wrapPhase(foldConstantLoweringPhase),
            wrapPhase(computeStringTrimPhase),
            wrapPhase(stringConcatenationPhase),
            wrapPhase(stringConcatenationTypeNarrowingPhase),
            wrapPhase(enumConstructorsPhase),
            wrapPhase(initializersPhase),
            wrapPhase(localFunctionsPhase),
            wrapPhase(tailrecPhase),
            wrapPhase(defaultParameterExtentPhase),
            wrapPhase(innerClassPhase),
            DataClassesPhase,
            wrapPhase(ifNullExpressionsFusionPhase),
            wrapPhase(testProcessorPhase),
            DelegationPhase,
            FunctionReferencePhase,
            wrapPhase(singleAbstractMethodPhase),
            wrapPhase(enumWhenPhase),
            BuiltinOperatorPhase,
            wrapPhase(finallyBlocksPhase),
            wrapPhase(enumClassPhase),
            wrapPhase(enumUsagePhase),
            InteropPhase,
            wrapPhase(varargPhase),
            wrapPhase(kotlinNothingValueExceptionPhase),
            CoroutinesPhase,
            wrapPhase(typeOperatorPhase),
            wrapPhase(expressionBodyTransformPhase),
            ObjectClassesPhase,
            wrapPhase(constantInliningPhase),
            wrapPhase(staticInitializersPhase),
            wrapPhase(bridgesPhase),
            wrapPhase(exportInternalAbiPhase),
            UseInternalAbiPhase,
            wrapPhase(autoboxPhase),
    )

    irModuleFragment.files.forEach { file ->
        context.fileLowerState = FileLowerState()
        allLowerings.forEach {
            runPhase(it, file)
        }
    }
}