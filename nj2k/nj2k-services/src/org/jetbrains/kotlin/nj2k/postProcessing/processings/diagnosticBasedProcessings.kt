/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.quickfix.NumberConversionFix
import org.jetbrains.kotlin.idea.quickfix.RemoveUselessCastFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.nj2k.postProcessing.diagnosticBasedProcessing
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isSignedOrUnsignedNumberType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

val fixValToVarDiagnosticBasedProcessing =
    diagnosticBasedProcessing(
        Errors.VAL_REASSIGNMENT, Errors.CAPTURED_VAL_INITIALIZATION, Errors.CAPTURED_MEMBER_VAL_INITIALIZATION
    ) { element: KtSimpleNameExpression, _ ->
        val property = element.mainReference.resolve() as? KtProperty ?: return@diagnosticBasedProcessing
        if (!property.isVar) {
            property.valOrVarKeyword.replace(KtPsiFactory(element.project).createVarKeyword())
        }
    }

val fixTypeMismatchDiagnosticBasedProcessing =
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
            element is KtLambdaExpression
                    && expectedType.isNothing() -> {
                for (valueParameter in element.valueParameters) {
                    valueParameter.typeReference?.delete()
                    valueParameter.colon?.delete()
                }
            }
        }
    }

val removeUselessCastDiagnosticBasedProcessing =
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
    }

val removeInnecessaryNotNullAssertionDiagnosticBasedProcessing =
    diagnosticBasedProcessing<KtSimpleNameExpression>(Errors.UNNECESSARY_NOT_NULL_ASSERTION) { element, _ ->
        val exclExclExpr = element.parent as KtUnaryExpression
        val baseExpression = exclExclExpr.baseExpression!!
        val context = baseExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
        if (context.diagnostics.forElement(element).any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }) {
            exclExclExpr.replace(baseExpression)
        }
    }