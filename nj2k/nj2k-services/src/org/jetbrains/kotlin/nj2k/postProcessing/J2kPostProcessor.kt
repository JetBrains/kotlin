/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToElvisInspection
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.idea.inspections.conventionNameCalls.ReplaceGetOrSetInspection
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.FoldIfToReturnAsymmetricallyIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.FoldIfToReturnIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isTrivialStatementBody
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.postProcessing.processings.*
import org.jetbrains.kotlin.psi.*

class NewJ2kPostProcessor : PostProcessor {
    @Suppress("PrivatePropertyName")
    private val LOG = Logger.getInstance("@org.jetbrains.kotlin.nj2k.postProcessing.NewJ2kPostProcessor")

    override fun insertImport(file: KtFile, fqName: FqName) {
        runUndoTransparentActionInEdt(inWriteAction = true) {
            val descriptors = file.resolveImportReference(fqName)
            descriptors.firstOrNull()?.let { ImportInsertHelper.getInstance(file.project).importDescriptor(file, it) }
        }
    }

    override val phasesCount = processings.size

    override fun doAdditionalProcessing(
        file: KtFile,
        converterContext: ConverterContext?,
        rangeMarker: RangeMarker?,
        onPhaseChanged: ((Int, String) -> Unit)?
    ) {
        for ((i, group) in processings.withIndex()) {
            onPhaseChanged?.invoke(i + 1, group.description)
            for (processing in group.processings) {
                try {
                    processing.runProcessing(file, rangeMarker, converterContext as NewJ2kConverterContext)
                } catch (e: ProcessCanceledException) {
                    throw e
                } catch (t: Throwable) {
                    LOG.error(t)
                } finally {
                    commitFile(file)
                }
            }
        }
    }

    private fun commitFile(file: KtFile) {
        runUndoTransparentActionInEdt(inWriteAction = true) {
            file.commitAndUnblockDocument()
        }
    }
}

private val errorsFixingDiagnosticBasedPostProcessingGroup =
    DiagnosticBasedPostProcessingGroup(
        diagnosticBasedProcessing(Errors.REDUNDANT_OPEN_IN_INTERFACE) { element: KtModifierListOwner, _ ->
            element.removeModifier(KtTokens.OPEN_KEYWORD)
        },
        diagnosticBasedProcessing(Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) { element: KtDotQualifiedExpression, _ ->
            val parent = element.parent as? KtImportDirective ?: return@diagnosticBasedProcessing
            parent.delete()
        },

        diagnosticBasedProcessing(
            addExclExclFactoryNoImplicitReceiver(AddExclExclCallFix),
            Errors.UNSAFE_CALL,
            Errors.UNSAFE_INFIX_CALL,
            Errors.UNSAFE_OPERATOR_CALL
        ),
        diagnosticBasedProcessing(
            addExclExclFactoryNoImplicitReceiver(MissingIteratorExclExclFixFactory),
            Errors.ITERATOR_ON_NULLABLE
        ),
        diagnosticBasedProcessing(
            SmartCastImpossibleExclExclFixFactory,
            Errors.SMARTCAST_IMPOSSIBLE
        ),

        diagnosticBasedProcessing(
            ReplacePrimitiveCastWithNumberConversionFix,
            Errors.CAST_NEVER_SUCCEEDS
        ),
        diagnosticBasedProcessing(
            ChangeCallableReturnTypeFix.ReturnTypeMismatchOnOverrideFactory,
            Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE
        ),

        diagnosticBasedProcessing(
            RemoveModifierFix.createRemoveProjectionFactory(true),
            Errors.REDUNDANT_PROJECTION
        ),
        diagnosticBasedProcessing(
            AddModifierFix.createFactory(KtTokens.OVERRIDE_KEYWORD),
            Errors.VIRTUAL_MEMBER_HIDDEN
        ),
        diagnosticBasedProcessing(
            RemoveModifierFix.createRemoveModifierFromListOwnerFactory(KtTokens.OPEN_KEYWORD),
            Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS, Errors.NON_FINAL_MEMBER_IN_OBJECT
        ),
        diagnosticBasedProcessing(
            MakeVisibleFactory,
            Errors.INVISIBLE_MEMBER
        ),
        diagnosticBasedProcessing(
            RemoveModifierFix.createRemoveModifierFactory(),
            Errors.WRONG_MODIFIER_TARGET
        ),
        fixValToVarDiagnosticBasedProcessing,
        fixTypeMismatchDiagnosticBasedProcessing
    )


private val addOrRemoveModifiersProcessingGroup =
    InspectionLikeProcessingGroup(
        runSingleTime = true,
        processings = listOf(
            RemoveRedundantVisibilityModifierProcessing(),
            RemoveRedundantModalityModifierProcessing(),
            inspectionBasedProcessing(AddOperatorModifierInspection(), writeActionNeeded = false),
            RemoveExplicitUnitTypeProcessing()
        )
    )

private val removeRedundantElementsProcessingGroup =
    InspectionLikeProcessingGroup(
        runSingleTime = true,
        processings = listOf(
            RemoveExplicitTypeArgumentsProcessing(),
            RemoveJavaStreamsCollectCallTypeArgumentsProcessing(),
            RedundantCompanionReferenceInspectionBasedProcessing(),
            ExplicitThisInspectionBasedProcessing(),
            intentionBasedProcessing(RemoveEmptyClassBodyIntention())
        )
    )

private val removeRedundantSemicolonProcessing =
    InspectionLikeProcessingGroup(
        runSingleTime = true,
        acceptNonKtElements = true,
        processings = listOf(
            RedundantSemicolonInspectionBasedProcessing()
        )
    )


private val inspectionLikePostProcessingGroup =
    InspectionLikeProcessingGroup(
        RemoveRedundantConstructorKeywordProcessing(),
        RemoveExplicitOpenInInterfaceProcessing(),
        RemoveRedundantOverrideVisibilityProcessing(),
        MoveLambdaOutsideParenthesesProcessing(),
        intentionBasedProcessing(ConvertToStringTemplateIntention(), writeActionNeeded = false) {
            ConvertToStringTemplateIntention.shouldSuggestToConvert(it)
        },
        intentionBasedProcessing(UsePropertyAccessSyntaxIntention(), writeActionNeeded = false),
        UninitializedVariableReferenceFromInitializerToThisReferenceProcessing(),
        UnresolvedVariableReferenceFromInitializerToThisReferenceProcessing(),
        RemoveRedundantSamAdaptersProcessing(),
        RemoveRedundantCastToNullableProcessing(),
        inspectionBasedProcessing(ReplacePutWithAssignmentInspection()),
        ReplaceGetterBodyWithSingleReturnStatementWithExpressionBody(),
        inspectionBasedProcessing(UnnecessaryVariableInspection(), writeActionNeeded = false),
        RedundantExplicitTypeInspectionBasedProcessing(),
        JavaObjectEqualsToEqOperatorProcessing(),
        RemoveExplicitPropertyTypeProcessing(),
        RemoveRedundantNullabilityProcessing(),
        CanBeValInspectionBasedProcessing(),
        inspectionBasedProcessing(FoldInitializerAndIfToElvisInspection(), writeActionNeeded = false),
        intentionBasedProcessing(RemoveRedundantCallsOfConversionMethodsIntention()),
        inspectionBasedProcessing(JavaMapForEachInspection()),
        intentionBasedProcessing(FoldIfToReturnIntention()) { it.then.isTrivialStatementBody() && it.`else`.isTrivialStatementBody() },
        intentionBasedProcessing(FoldIfToReturnAsymmetricallyIntention()) {
            it.then.isTrivialStatementBody() && (KtPsiUtil.skipTrailingWhitespacesAndComments(
                it
            ) as KtReturnExpression).returnedExpression.isTrivialStatementBody()
        },
        inspectionBasedProcessing(IfThenToSafeAccessInspection(), writeActionNeeded = false),
        inspectionBasedProcessing(IfThenToElvisInspection(highlightStatement = true), writeActionNeeded = false),
        inspectionBasedProcessing(SimplifyNegatedBinaryExpressionInspection()),
        inspectionBasedProcessing(ReplaceGetOrSetInspection()),
        intentionBasedProcessing(ObjectLiteralToLambdaIntention(), writeActionNeeded = true),
        intentionBasedProcessing(RemoveUnnecessaryParenthesesIntention()),
        intentionBasedProcessing(DestructureIntention(), writeActionNeeded = false),
        inspectionBasedProcessing(SimplifyAssertNotNullInspection()),
        intentionBasedProcessing(RemoveRedundantCallsOfConversionMethodsIntention()),
        LiftReturnInspectionBasedProcessing(),
        LiftAssignmentInspectionBasedProcessing(),
        intentionBasedProcessing(RemoveEmptyPrimaryConstructorIntention()),
        MayBeConstantInspectionBasedProcessing(),
        RemoveForExpressionLoopParameterTypeProcessing(),
        intentionBasedProcessing(ReplaceMapGetOrDefaultIntention()),
        inspectionBasedProcessing(ReplaceGuardClauseWithFunctionCallInspection())
    )


private val cleaningUpDiagnosticBasedPostProcessingGroup =
    DiagnosticBasedPostProcessingGroup(
        removeUselessCastDiagnosticBasedProcessing,
        removeInnecessaryNotNullAssertionDiagnosticBasedProcessing,
        fixValToVarDiagnosticBasedProcessing
    )


private val processings: List<NamedPostProcessingGroup> = listOf(
    NamedPostProcessingGroup(
        "Inferring declarations nullability",
        listOf(
            InspectionLikeProcessingGroup(
                processings = listOf(
                    VarToValProcessing(),
                    CanBeValInspectionBasedProcessing()
                ),
                runSingleTime = true
            ),
            NullabilityInferenceProcessing(),
            MutabilityInferenceProcessing(),
            ClearUnknownLabelsProcessing()
        )
    ),
    NamedPostProcessingGroup(
        "Formatting code",
        listOf(FormatCodeProcessing())
    ),
    NamedPostProcessingGroup(
        "Shortening fully-qualified references",
        listOf(ShortenReferenceProcessing())
    ),
    NamedPostProcessingGroup(
        "Converting POJOs to data classes",
        listOf(
            InspectionLikeProcessingGroup(VarToValProcessing()),
            ConvertGettersAndSettersToPropertyProcessing(),
            InspectionLikeProcessingGroup(MoveGetterAndSetterAnnotationsToPropertyProcessing()),
            InspectionLikeProcessingGroup(
                RemoveExplicitGetterInspectionBasedProcessing(),
                RemoveExplicitSetterInspectionBasedProcessing()
            ),
            ConvertToDataClassProcessing()
        )
    ),
    NamedPostProcessingGroup(
        "Cleaning up Kotlin code",
        listOf(
            errorsFixingDiagnosticBasedPostProcessingGroup,
            addOrRemoveModifiersProcessingGroup,
            inspectionLikePostProcessingGroup,
            removeRedundantSemicolonProcessing,
            removeRedundantElementsProcessingGroup,
            cleaningUpDiagnosticBasedPostProcessingGroup
        )
    ),
    NamedPostProcessingGroup(
        "Optimizing imports",
        listOf(
            OptimizeImportsProcessing(),
            ShortenReferenceProcessing()
        )
    ),
    NamedPostProcessingGroup(
        "Formatting code",
        listOf(FormatCodeProcessing())
    )
)