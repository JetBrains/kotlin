/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.analysis

import com.intellij.lang.Language
import javaslang.control.Option
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.util.javaslang.component1
import org.jetbrains.kotlin.util.javaslang.component2
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.analysis.UExpressionFact
import org.jetbrains.uast.analysis.UNullability
import org.jetbrains.uast.analysis.UastAnalysisPlugin
import org.jetbrains.uast.kotlin.analyze

@Suppress("UnstableApiUsage")
class KotlinUastAnalysisPlugin : UastAnalysisPlugin {
    override val language: Language
        get() = KotlinLanguage.INSTANCE

    private fun KtReferenceExpression.getVariableDescriptor(context: BindingContext): VariableDescriptor? {
        return context[BindingContext.REFERENCE_TARGET, this] as? VariableDescriptor
    }

    private fun isValueCorrespondsToDescriptor(value: DataFlowValue, descriptor: VariableDescriptor?) =
        when (val identifierInfo = value.identifierInfo) {
            is IdentifierInfo.Variable -> identifierInfo.variable == descriptor
            is IdentifierInfo.Qualified ->
                (identifierInfo.selectorInfo as? IdentifierInfo.Variable)?.let { it.variable == descriptor } == true
            else -> false
        }

    override fun <T : Any> UExpression.getExpressionFact(fact: UExpressionFact<T>): T? {
        val psiExpression = sourcePsi as? KtExpression ?: return null

        @Suppress("UNCHECKED_CAST")
        return when (fact) {
            UExpressionFact.UNullabilityFact -> {
                val typeInfo = psiExpression.analyze()[BindingContext.EXPRESSION_TYPE_INFO, psiExpression]
                val dfaInfo = typeInfo?.dataFlowInfo
                val variableDescriptor = (psiExpression as? KtReferenceExpression)?.getVariableDescriptor(psiExpression.analyze())

                val variableNullability = dfaInfo?.completeNullabilityInfo?.find { (value, _) ->
                    isValueCorrespondsToDescriptor(value, variableDescriptor)
                }
                when {
                    typeInfo?.type is SimpleType && typeInfo.type?.isMarkedNullable == false -> UNullability.NOT_NULL
                    variableNullability is Option.Some<*> -> {
                        val (_, info) = variableNullability.get()
                        when (info) {
                            Nullability.NULL -> UNullability.NULL
                            Nullability.NOT_NULL -> UNullability.NOT_NULL
                            else -> UNullability.UNKNOWN
                        }
                    }
                    else -> {
                        val type = typeInfo?.type
                        when {
                            type is FlexibleType -> {
                                when {
                                    !type.lowerBound.isMarkedNullable && type.upperBound.isMarkedNullable -> UNullability.UNKNOWN
                                    type.upperBound.isMarkedNullable -> UNullability.NULLABLE
                                    else -> UNullability.NOT_NULL
                                }
                            }
                            type?.isMarkedNullable == true -> UNullability.NULLABLE
                            else -> UNullability.NOT_NULL
                        }
                    }
                }
            }
        } as T
    }
}