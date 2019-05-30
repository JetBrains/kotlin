/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.idea.inspections.conventionNameCalls.ReplaceGetOrSetInspection
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.FoldIfToReturnAsymmetricallyIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.FoldIfToReturnIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isTrivialStatementBody
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.postProcessing.processings.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isSignedOrUnsignedNumberType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class NewJ2kPostProcessor : PostProcessor {
    override fun insertImport(file: KtFile, fqName: FqName) {
        ApplicationManager.getApplication().invokeAndWait {
            runWriteAction {
                val descriptors = file.resolveImportReference(fqName)
                descriptors.firstOrNull()?.let { ImportInsertHelper.getInstance(file.project).importDescriptor(file, it) }
            }
        }
    }

    override fun doAdditionalProcessing(file: KtFile, converterContext: ConverterContext?, rangeMarker: RangeMarker?) {
        runBlocking(EDT.ModalityStateElement(ModalityState.defaultModalityState())) {
            for (processing in processings) {
                processing.runProcessing(file, rangeMarker, converterContext as NewJ2kConverterContext)
            }
        }
    }
}

private val processings: List<GeneralPostProcessing> = listOf(
    nullabilityProcessing,
    formatCodeProcessing,
    shortenReferencesProcessing,
    InspectionLikeProcessingGroup(VarToValProcessing()),
    ConvertGettersAndSettersToPropertyProcessing(),
    InspectionLikeProcessingGroup(MoveGetterAndSetterAnnotationsToPropertyProcessing()),
    InspectionLikeProcessingGroup(
        generalInspectionBasedProcessing(RedundantGetterInspection()),
        generalInspectionBasedProcessing(RedundantSetterInspection())
    ),
    ConvertToDataClassProcessing(),
    InspectionLikeProcessingGroup(
        RemoveRedundantVisibilityModifierProcessing(),
        RemoveRedundantModalityModifierProcessing(),
        RemoveRedundantConstructorKeywordProcessing(),
        diagnosticBasedProcessing(Errors.REDUNDANT_OPEN_IN_INTERFACE) { element: KtDeclaration, _ ->
            element.removeModifier(KtTokens.OPEN_KEYWORD)
        },
        RemoveExplicitOpenInInterfaceProcessing(),
        generalInspectionBasedProcessing(ExplicitThisInspection()),
        RemoveExplicitTypeArgumentsProcessing(),
        RemoveRedundantOverrideVisibilityProcessing(),
        inspectionBasedProcessing(MoveLambdaOutsideParenthesesInspection()),
        generalInspectionBasedProcessing(RedundantCompanionReferenceInspection()),
        FixObjectStringConcatenationProcessing(),
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
        generalInspectionBasedProcessing(RedundantUnitReturnTypeInspection()),
        JavaObjectEqualsToEqOperatorProcessing(),
        RemoveExplicitPropertyTypeProcessing(),
        RemoveRedundantNullabilityProcessing(),
        generalInspectionBasedProcessing(CanBeValInspection(ignoreNotUsedVals = false)),
        intentionBasedProcessing(FoldInitializerAndIfToElvisIntention()),
        generalInspectionBasedProcessing(RedundantSemicolonInspection()),
        intentionBasedProcessing(RemoveEmptyClassBodyIntention()),
        intentionBasedProcessing(
            RemoveRedundantCallsOfConversionMethodsIntention()
        ),
        inspectionBasedProcessing(JavaMapForEachInspection()),

        intentionBasedProcessing(FoldIfToReturnIntention()) { it.then.isTrivialStatementBody() && it.`else`.isTrivialStatementBody() },
        intentionBasedProcessing(FoldIfToReturnAsymmetricallyIntention()) {
            it.then.isTrivialStatementBody() && (KtPsiUtil.skipTrailingWhitespacesAndComments(
                it
            ) as KtReturnExpression).returnedExpression.isTrivialStatementBody()
        },

        inspectionBasedProcessing(IfThenToSafeAccessInspection()),
        inspectionBasedProcessing(IfThenToSafeAccessInspection()),
        intentionBasedProcessing(IfThenToElvisIntention()),
        inspectionBasedProcessing(SimplifyNegatedBinaryExpressionInspection()),
        inspectionBasedProcessing(ReplaceGetOrSetInspection()),
        intentionBasedProcessing(AddOperatorModifierIntention()),
        intentionBasedProcessing(ObjectLiteralToLambdaIntention()),
        intentionBasedProcessing(AnonymousFunctionToLambdaIntention()),
        intentionBasedProcessing(RemoveUnnecessaryParenthesesIntention()),
        intentionBasedProcessing(DestructureIntention()),
        inspectionBasedProcessing(SimplifyAssertNotNullInspection()),
        intentionBasedProcessing(
            RemoveRedundantCallsOfConversionMethodsIntention()
        ),
        generalInspectionBasedProcessing(LiftReturnOrAssignmentInspection(skipLongExpressions = false)),
        generalInspectionBasedProcessing(MayBeConstantInspection()),
        intentionBasedProcessing(RemoveEmptyPrimaryConstructorIntention()),
        diagnosticBasedProcessing(Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) { element: KtDotQualifiedExpression, _ ->
            val parent = element.parent as? KtImportDirective ?: return@diagnosticBasedProcessing
            parent.delete()
        },

        diagnosticBasedProcessing(
            Errors.UNSAFE_CALL,
            Errors.UNSAFE_INFIX_CALL,
            Errors.UNSAFE_OPERATOR_CALL
        ) { element: PsiElement, diagnostic ->
            val action =
                AddExclExclCallFix.createActions(diagnostic).singleOrNull()
                    ?: return@diagnosticBasedProcessing
            action.invoke(element.project, null, element.containingFile)
        },

        diagnosticBasedProcessingWithFixFactory(
            MissingIteratorExclExclFixFactory,
            Errors.ITERATOR_ON_NULLABLE
        ),
        diagnosticBasedProcessingWithFixFactory(
            SmartCastImpossibleExclExclFixFactory,
            Errors.SMARTCAST_IMPOSSIBLE
        ),
        diagnosticBasedProcessing(Errors.TYPE_MISMATCH) { element: PsiElement, diagnostic ->
            @Suppress("UNCHECKED_CAST")
            val diagnosticWithParameters =
                diagnostic as? DiagnosticWithParameters2<KtExpression, KotlinType, KotlinType>
                    ?: return@diagnosticBasedProcessing
            val expectedType = diagnosticWithParameters.a
            val realType = diagnosticWithParameters.b
            when {
                realType.makeNotNullable().isSubtypeOf(expectedType.makeNotNullable())
                        && realType.isNullable()
                        && !expectedType.isNullable()
                -> {
                    val factory = KtPsiFactory(element)
                    element.replace(factory.createExpressionByPattern("($0)!!", element.text))
                }
                element is KtExpression
                        && realType.isSignedOrUnsignedNumberType()
                        && expectedType.isSignedOrUnsignedNumberType() -> {
                    val fix = NumberConversionFix(element, expectedType, disableIfAvailable = null)
                    fix.invoke(element.project, null, element.containingFile)
                }
            }
        },

        diagnosticBasedProcessingWithFixFactory(
            ReplacePrimitiveCastWithNumberConversionFix,
            Errors.CAST_NEVER_SUCCEEDS
        ),
        diagnosticBasedProcessingWithFixFactory(
            ChangeCallableReturnTypeFix.ReturnTypeMismatchOnOverrideFactory,
            Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE
        ),

        diagnosticBasedProcessing<KtBinaryExpressionWithTypeRHS>(Errors.USELESS_CAST) { element, _ ->
            if (element.left.isNullExpression()) return@diagnosticBasedProcessing
            val expression = RemoveUselessCastFix.invoke(element)

            val variable = expression.parent as? KtProperty
            if (variable != null && expression == variable.initializer && variable.isLocal) {
                val ref = ReferencesSearch.search(variable, LocalSearchScope(variable.containingFile)).findAll().singleOrNull()
                if (ref != null && ref.element is KtSimpleNameExpression) {
                    ref.element.replace(expression)
                    variable.delete()
                }
            }
        },

        diagnosticBasedProcessingWithFixFactory(
            RemoveModifierFix.createRemoveProjectionFactory(true),
            Errors.REDUNDANT_PROJECTION
        ),
        diagnosticBasedProcessingWithFixFactory(
            AddModifierFix.createFactory(KtTokens.OVERRIDE_KEYWORD),
            Errors.VIRTUAL_MEMBER_HIDDEN
        ),
        diagnosticBasedProcessingWithFixFactory(
            RemoveModifierFix.createRemoveModifierFromListOwnerFactory(KtTokens.OPEN_KEYWORD),
            Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS, Errors.NON_FINAL_MEMBER_IN_OBJECT
        ),
        diagnosticBasedProcessingWithFixFactory(
            MakeVisibleFactory,
            Errors.INVISIBLE_MEMBER
        ),

        diagnosticBasedProcessingFactory(
            Errors.VAL_REASSIGNMENT, Errors.CAPTURED_VAL_INITIALIZATION, Errors.CAPTURED_MEMBER_VAL_INITIALIZATION
        ) { element: KtSimpleNameExpression, _: Diagnostic ->
            val property = element.mainReference.resolve() as? KtProperty
            if (property == null) {
                null
            } else {
                val action = {
                    if (!property.isVar) {
                        property.valOrVarKeyword.replace(KtPsiFactory(element.project).createVarKeyword())
                    }
                }
                action
            }
        },

        diagnosticBasedProcessing<KtSimpleNameExpression>(Errors.UNNECESSARY_NOT_NULL_ASSERTION) { element, _ ->
            val exclExclExpr = element.parent as KtUnaryExpression
            val baseExpression = exclExclExpr.baseExpression!!
            val context = baseExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
            if (context.diagnostics.forElement(element).any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }) {
                exclExclExpr.replace(baseExpression)
            }
        },
        RemoveForExpressionLoopParameterTypeProcessing()
    ),
    formatCodeProcessing,
    optimizeImportsProcessing
)