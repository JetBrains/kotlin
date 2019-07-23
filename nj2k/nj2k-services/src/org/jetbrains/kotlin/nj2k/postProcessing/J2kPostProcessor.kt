/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.ProcessCanceledException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.util.EDT
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
import org.jetbrains.kotlin.idea.util.application.runWriteAction
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
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                val descriptors = file.resolveImportReference(fqName)
                descriptors.firstOrNull()?.let { ImportInsertHelper.getInstance(file.project).importDescriptor(file, it) }
            }
        }
    }

    override val phasesCount = processings.size

    override fun doAdditionalProcessing(
        file: KtFile,
        converterContext: ConverterContext?,
        rangeMarker: RangeMarker?,
        onPhaseChanged: ((Int, String) -> Unit)?
    ) {
        runBlocking(EDT.ModalityStateElement(ModalityState.defaultModalityState())) {
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
    }

    private suspend fun commitFile(file: KtFile) {
        withContext(EDT) {
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    file.commitAndUnblockDocument()
                }
            }
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
            inspectionBasedProcessing(AddOperatorModifierInspection()),
            generalInspectionBasedProcessing(RedundantUnitReturnTypeInspection())
        )
    )

private val removeRedundantElementsProcessingGroup =
    InspectionLikeProcessingGroup(
        runSingleTime = true,
        processings = listOf(
            RemoveExplicitTypeArgumentsProcessing(),
            generalInspectionBasedProcessing(RedundantCompanionReferenceInspection()),
            generalInspectionBasedProcessing(ExplicitThisInspection()),
            intentionBasedProcessing(RemoveEmptyClassBodyIntention())
        )
    )

private val removeRedundantSemicolonProcessing =
    InspectionLikeProcessingGroup(
        runSingleTime = true,
        acceptNonKtElements = true,
        processings = listOf(
            generalInspectionBasedProcessing(RedundantSemicolonInspection())
        )
    )


private val inspectionLikePostProcessingGroup =
    InspectionLikeProcessingGroup(
        RemoveRedundantConstructorKeywordProcessing(),
        RemoveExplicitOpenInInterfaceProcessing(),
        RemoveRedundantOverrideVisibilityProcessing(),
        inspectionBasedProcessing(MoveLambdaOutsideParenthesesInspection()),
        ConvertToStringTemplateProcessing(),
        UsePropertyAccessSyntaxProcessing(),
        UninitializedVariableReferenceFromInitializerToThisReferenceProcessing(),
        UnresolvedVariableReferenceFromInitializerToThisReferenceProcessing(),
        RemoveRedundantSamAdaptersProcessing(),
        RemoveRedundantCastToNullableProcessing(),
        inspectionBasedProcessing(ReplacePutWithAssignmentInspection()),
        UseExpressionBodyProcessing(),
        inspectionBasedProcessing(UnnecessaryVariableInspection()),
        RemoveExplicitPropertyTypeWithInspectionProcessing(),
        JavaObjectEqualsToEqOperatorProcessing(),
        RemoveExplicitPropertyTypeProcessing(),
        RemoveRedundantNullabilityProcessing(),
        generalInspectionBasedProcessing(CanBeValInspection(ignoreNotUsedVals = false)),
        inspectionBasedProcessing(FoldInitializerAndIfToElvisInspection()),
        intentionBasedProcessing(RemoveRedundantCallsOfConversionMethodsIntention()),
        inspectionBasedProcessing(JavaMapForEachInspection()),
        intentionBasedProcessing(FoldIfToReturnIntention()) { it.then.isTrivialStatementBody() && it.`else`.isTrivialStatementBody() },
        intentionBasedProcessing(FoldIfToReturnAsymmetricallyIntention()) {
            it.then.isTrivialStatementBody() && (KtPsiUtil.skipTrailingWhitespacesAndComments(
                it
            ) as KtReturnExpression).returnedExpression.isTrivialStatementBody()
        },
        inspectionBasedProcessing(IfThenToSafeAccessInspection()),
        inspectionBasedProcessing(IfThenToElvisInspection(highlightStatement = true)),
        inspectionBasedProcessing(SimplifyNegatedBinaryExpressionInspection()),
        inspectionBasedProcessing(ReplaceGetOrSetInspection()),
        intentionBasedProcessing(ObjectLiteralToLambdaIntention()),
        intentionBasedProcessing(AnonymousFunctionToLambdaIntention()),
        intentionBasedProcessing(RemoveUnnecessaryParenthesesIntention()),
        intentionBasedProcessing(DestructureIntention()),
        inspectionBasedProcessing(SimplifyAssertNotNullInspection()),
        intentionBasedProcessing(RemoveRedundantCallsOfConversionMethodsIntention()),
        generalInspectionBasedProcessing(LiftReturnOrAssignmentInspection(skipLongExpressions = false)),
        intentionBasedProcessing(RemoveEmptyPrimaryConstructorIntention()),
        generalInspectionBasedProcessing(MayBeConstantInspection()),
        RemoveForExpressionLoopParameterTypeProcessing()
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
        listOf(nullabilityProcessing)
    ),
    NamedPostProcessingGroup(
        "Formatting code",
        listOf(formatCodeProcessing)
    ),
    NamedPostProcessingGroup(
        "Shortening fully-qualified references",
        listOf(shortenReferencesProcessing)
    ),
    NamedPostProcessingGroup(
        "Converting POJOs to data classes",
        listOf(
            InspectionLikeProcessingGroup(VarToValProcessing()),
            ConvertGettersAndSettersToPropertyProcessing(),
            InspectionLikeProcessingGroup(MoveGetterAndSetterAnnotationsToPropertyProcessing()),
            InspectionLikeProcessingGroup(
                generalInspectionBasedProcessing(RedundantGetterInspection()),
                generalInspectionBasedProcessing(RedundantSetterInspection())
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
            optimizeImportsProcessing,
            shortenReferencesProcessing
        )
    ),
    NamedPostProcessingGroup(
        "Formatting code",
        listOf(formatCodeProcessing)
    )
)