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
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.UastVisitor

open class KotlinUSimpleReferenceExpression(
        override val psi: KtSimpleNameExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    private val resolvedDeclaration by lz { psi.resolveCallToDeclaration(this) }

    override val identifier get() = psi.getReferencedName()

    override fun resolve() = resolvedDeclaration

    override val resolvedName: String?
        get() = (resolvedDeclaration as? PsiNamedElement)?.name

    override fun accept(visitor: UastVisitor) {
        visitor.visitSimpleNameReferenceExpression(this)

        if (psi.parent.destructuringDeclarationInitializer != true) {
            visitAccessorCalls(visitor)
        }

        visitor.afterVisitSimpleNameReferenceExpression(this)
    }

    private fun visitAccessorCalls(visitor: UastVisitor) {
        // Visit Kotlin get-set synthetic Java property calls as function calls
        val bindingContext = psi.analyze()
        val access = psi.readWriteAccess()
        val resolvedCall = psi.getResolvedCall(bindingContext)
        val resultingDescriptor = resolvedCall?.resultingDescriptor as? SyntheticJavaPropertyDescriptor
        if (resultingDescriptor != null) {
            val setterValue = if (access.isWrite) {
                findAssignment(psi, psi.parent)?.right ?: run {
                    visitor.afterVisitSimpleNameReferenceExpression(this)
                    return
                }
            } else {
                null
            }

            if (resolvedCall != null) {
                if (access.isRead) {
                    val getDescriptor = resultingDescriptor.getMethod
                    KotlinAccessorCallExpression(psi, this, resolvedCall, getDescriptor, null).accept(visitor)
                }

                if (access.isWrite && setterValue != null) {
                    val setDescriptor = resultingDescriptor.setMethod
                    if (setDescriptor != null) {
                        KotlinAccessorCallExpression(psi, this, resolvedCall, setDescriptor, setterValue).accept(visitor)
                    }
                }
            }
        }
    }

    private tailrec fun findAssignment(prev: PsiElement?, element: PsiElement?): KtBinaryExpression? = when (element) {
        is KtBinaryExpression -> if (element.left == prev && element.operationToken == KtTokens.EQ) element else null
        is KtQualifiedExpression -> findAssignment(element, element.parent)
        is KtSimpleNameExpression -> findAssignment(element, element.parent)
        else -> null
    }

    class KotlinAccessorCallExpression(
            override val psi: KtElement,
            override val uastParent: KotlinUSimpleReferenceExpression,
            private val resolvedCall: ResolvedCall<*>,
            private val accessorDescriptor: DeclarationDescriptor,
            val setterValue: KtExpression?
    ) : UCallExpression {
        override val methodName: String?
            get() = accessorDescriptor.name.asString()

        override val receiver: UExpression?
            get() {
                val containingElement = uastParent.uastParent
                return if (containingElement is UQualifiedReferenceExpression && containingElement.selector == this)
                    containingElement.receiver
                else
                    null
            }

        override val annotations: List<UAnnotation>
            get() = emptyList()

        override val receiverType by lz {
            val type = (resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver)?.type ?: return@lz null
            type.toPsiType(this, psi, boxed = true)
        }

        override val methodIdentifier: UIdentifier?
            get() = UIdentifier(uastParent.psi, this)

        override val classReference: UReferenceExpression?
            get() = null

        override val valueArgumentCount: Int
            get() = if (setterValue != null) 1 else 0

        override val valueArguments by lz {
            if (setterValue != null)
                listOf(KotlinConverter.convertOrEmpty(setterValue, this))
            else
                emptyList()
        }

        override val typeArgumentCount: Int
            get() = resolvedCall.typeArguments.size

        override val typeArguments by lz {
            resolvedCall.typeArguments.values.map { it.toPsiType(this, psi, true) }
        }

        override val returnType by lz {
            (accessorDescriptor as? CallableDescriptor)?.returnType?.toPsiType(this, psi, boxed = false)
        }

        override val kind: UastCallKind
            get() = UastCallKind.METHOD_CALL

        override fun resolve(): PsiMethod? {
            val source = accessorDescriptor.toSource()
            return KotlinUFunctionCallExpression.resolveSource(accessorDescriptor, source)
        }
    }

    enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
        READ(true, false), WRITE(false, true), READ_WRITE(true, true)
    }

    private fun KtExpression.readWriteAccess(): ReferenceAccess {
        var expression = getQualifiedExpressionForSelectorOrThis()
        loop@ while (true) {
            val parent = expression.parent
            when (parent) {
                is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression = parent as KtExpression
                else -> break@loop
            }
        }

        val assignment = expression.getAssignmentByLHS()
        if (assignment != null) {
            return when (assignment.operationToken) {
                KtTokens.EQ -> ReferenceAccess.WRITE
                else -> ReferenceAccess.READ_WRITE
            }
        }

        return if ((expression.parent as? KtUnaryExpression)?.operationToken
                in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) })
            ReferenceAccess.READ_WRITE
        else
            ReferenceAccess.READ
    }
}

class KotlinClassViaConstructorUSimpleReferenceExpression(
        override val psi: KtCallElement,
        override val identifier: String,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression, KotlinUElementWithType {
    override val resolvedName: String?
        get() = (psi.getResolvedCall(psi.analyze())?.resultingDescriptor as? ConstructorDescriptor)
                ?.containingDeclaration?.name?.asString()

    override fun resolve(): PsiElement? {
        val resolvedCall = psi.getResolvedCall(psi.analyze())
        val resultingDescriptor = resolvedCall?.resultingDescriptor as? ConstructorDescriptor ?: return null
        val clazz = resultingDescriptor.containingDeclaration
        return clazz.toSource()?.getMaybeLightElement(this)
    }
}

class KotlinStringUSimpleReferenceExpression(
        override val identifier: String,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression {
    override val psi: PsiElement?
        get() = null
    override fun resolve() = null
    override val resolvedName: String?
        get() = identifier
}