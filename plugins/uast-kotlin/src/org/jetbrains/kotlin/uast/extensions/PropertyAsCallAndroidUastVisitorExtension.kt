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

package org.jetbrains.kotlin.uast.extensions

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.uast.*
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked
import org.jetbrains.uast.visitor.UastVisitor

class PropertyAsCallAndroidUastVisitorExtension : UastVisitorExtension {
    override fun invoke(element: UElement, visitor: UastVisitor, context: UastContext) {
        val expr = element as? KotlinUSimpleReferenceExpression ?: return
        if (expr is KotlinNameUSimpleReferenceExpression) return

        val ktElement = expr.psi as? KtElement ?: return
        val bindingContext = ktElement.analyze(BodyResolveMode.PARTIAL)

        val referenceToAccessor = ktElement.references.firstOrNull { it is SyntheticPropertyAccessorReference } ?: return
        val accessorDescriptor = (referenceToAccessor as SyntheticPropertyAccessorReference)
                                         .resolveToDescriptors(bindingContext).firstOrNull() ?: return

        val resolvedCall = ktElement.getResolvedCall(bindingContext) ?: return

        val setterValue = if (referenceToAccessor is SyntheticPropertyAccessorReference.Setter)
                findAssignment(ktElement, ktElement.parent)?.right ?: return
        else
            null

        val callExpression: UCallExpression = object : UCallExpression, PsiElementBacked, SynthesizedUElement {
            override val parent = element.parent
            override val psi = ktElement

            override val functionReference = KotlinNameUSimpleReferenceExpression(
                    expr.psi, expr.identifier, expr.parent, accessorDescriptor)

            override val classReference = null
            override val functionName = accessorDescriptor.name.asString()
            override val functionNameElement by lz { KotlinDumbUElement(ktElement, this) }

            override val valueArgumentCount: Int
                get() = if (setterValue != null) 1 else 0

            override val valueArguments by lz {
                if (setterValue != null)
                    listOf(KotlinConverter.convert(setterValue, this))
                else
                    emptyList()
            }

            override val typeArgumentCount: Int
                get() = resolvedCall.typeArguments.size

            override val typeArguments by lz {
                resolvedCall.typeArguments.map { KotlinUType(it.value, ktElement.project, this) }
            }

            override val kind = UastCallKind.FUNCTION_CALL

            override fun resolve(context: UastContext): UFunction? {
                val source = accessorDescriptor.toSource()
                if (source != null) {
                    (context.convert(source) as? UFunction)?.let { return it }
                }
                return element.resolveIfCan(context) as? UFunction
            }
        }

        visitor.visitCallExpression(callExpression)
    }

    private tailrec fun findAssignment(prev: PsiElement?, element: PsiElement?): KtBinaryExpression? = when (element) {
        is KtBinaryExpression -> if (element.left == prev && element.operationToken == KtTokens.EQ) element else null
        is KtQualifiedExpression -> findAssignment(element, element.parent)
        is KtSimpleNameExpression -> findAssignment(element, element.parent)
        else -> null
    }
}