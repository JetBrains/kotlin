/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections.collections

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

abstract class AbstractUselessCallInspection : AbstractKotlinInspection() {

    protected abstract val uselessFqNames: Map<String, Conversion>

    protected abstract val uselessNames: Set<String>

    protected abstract fun QualifiedExpressionVisitor.suggestConversionIfNeeded(
        expression: KtQualifiedExpression,
        calleeExpression: KtExpression,
        context: BindingContext,
        conversion: Conversion
    )

    inner class QualifiedExpressionVisitor internal constructor(val holder: ProblemsHolder, val isOnTheFly: Boolean) : KtVisitorVoid() {
        override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
            super.visitQualifiedExpression(expression)
            val selector = expression.selectorExpression as? KtCallExpression ?: return
            val calleeExpression = selector.calleeExpression ?: return
            if (calleeExpression.text !in uselessNames) return

            val context = expression.analyze()
            val resolvedCall = expression.getResolvedCall(context) ?: return
            val conversion = uselessFqNames[resolvedCall.resultingDescriptor.fqNameOrNull()?.asString()] ?: return

            suggestConversionIfNeeded(expression, calleeExpression, context, conversion)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = QualifiedExpressionVisitor(holder, isOnTheFly)

    protected data class Conversion(val replacementName: String? = null)

    protected companion object {

        val deleteConversion = Conversion()

        fun Set<String>.toShortNames() = mapTo(mutableSetOf()) { fqName -> fqName.takeLastWhile { it != '.' } }
    }
}