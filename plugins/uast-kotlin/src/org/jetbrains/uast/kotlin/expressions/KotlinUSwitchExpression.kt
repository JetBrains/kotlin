/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.kinds.KotlinSpecialExpressionKinds

class KotlinUSwitchExpression(
        override val psi: KtWhenExpression,
        override val containingElement: UElement?
) : KotlinAbstractUExpression(), USwitchExpression, KotlinUElementWithType {
    override val expression by lz { KotlinConverter.convertOrNull(psi.subjectExpression, this) }

    override val body: UExpressionList by lz {
        object : KotlinUExpressionList(psi, KotlinSpecialExpressionKinds.WHEN, this) {
            override fun asRenderString() = expressions.joinToString("\n") { it.asRenderString().withMargin }
        }.apply {
            expressions = this@KotlinUSwitchExpression.psi.entries.map { KotlinUSwitchEntry(it, this) }
        }
    }

    override fun asRenderString() = buildString {
        val expr = expression?.let { "(" + it.asRenderString() + ") " } ?: ""
        appendln("switch $expr {")
        appendln(body.asRenderString())
        appendln("}")
    }

    override val switchIdentifier: UIdentifier
        get() = UIdentifier(null, this)
}

class KotlinUSwitchEntry(
        override val psi: KtWhenEntry,
        override val containingElement: UExpression
) : KotlinAbstractUExpression(), USwitchClauseExpressionWithBody {
    override val caseValues by lz {
        psi.conditions.map { when (it) {
            is KtWhenConditionInRange -> KotlinCustomUBinaryExpression(it, this).apply {
                leftOperand = KotlinStringUSimpleReferenceExpression("it", this)
                operator = when {
                    it.isNegated -> KotlinBinaryOperators.NOT_IN
                    else -> KotlinBinaryOperators.IN
                }
                rightOperand = KotlinConverter.convertOrEmpty(it.rangeExpression, this)
            }
            is KtWhenConditionIsPattern -> KotlinCustomUBinaryExpressionWithType(it, this).apply {
                operand = KotlinStringUSimpleReferenceExpression("it", this)
                operationKind = when {
                    it.isNegated -> KotlinBinaryExpressionWithTypeKinds.NEGATED_INSTANCE_CHECK
                    else -> UastBinaryExpressionWithTypeKind.INSTANCE_CHECK
                }
                val typeRef = it.typeReference
                typeReference = typeRef?.let {
                    LazyKotlinUTypeReferenceExpression(it, this) { typeRef.toPsiType(this, boxed = true) }
                }
            }
            is KtWhenConditionWithExpression -> KotlinConverter.convertOrEmpty(it.expression, this)
            else -> UastEmptyExpression
        }}
    }

    override val body: UExpressionList by lz {
        object : KotlinUExpressionList(psi, KotlinSpecialExpressionKinds.WHEN_ENTRY, this) {
            override fun asRenderString() = buildString {
                appendln("{")
                expressions.forEach { appendln(it.asRenderString().withMargin) }
                appendln("}")
            }
        }.apply {
            val exprPsi = this@KotlinUSwitchEntry.psi.expression
            val userExpressions = when (exprPsi) {
                is KtBlockExpression -> exprPsi.statements.map { KotlinConverter.convertOrEmpty(it, this) }
                else -> listOf(KotlinConverter.convertOrEmpty(exprPsi, this))
            }
            containingElement
            expressions = userExpressions + object : UBreakExpression {
                override val psi: PsiElement?
                    get() = null
                override val label: String?
                    get() = null
                override val containingElement: UElement?
                    get() = this@KotlinUSwitchEntry
                override val annotations: List<UAnnotation>
                    get() = emptyList()
            }
        }
    }
}