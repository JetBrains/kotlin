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
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier
import org.jetbrains.uast.visitor.UastVisitor

class KotlinUFunctionCallExpression(
        override val psi: KtCallElement,
        givenParent: UElement?,
        private val _resolvedCall: ResolvedCall<*>?
) : KotlinAbstractUExpression(givenParent), UCallExpression, KotlinUElementWithType {
    companion object {
        fun resolveSource(descriptor: DeclarationDescriptor, source: PsiElement?): PsiMethod? {
            if (descriptor is ConstructorDescriptor && descriptor.isPrimary
                    && source is KtClassOrObject && source.primaryConstructor == null
                    && source.secondaryConstructors.isEmpty()) {
                return source.toLightClass()?.constructors?.firstOrNull()
            }

            return when (source) {
                is KtFunction -> LightClassUtil.getLightClassMethod(source)
                is PsiMethod -> source
                else -> null
            }
        }
    }

    constructor(psi: KtCallElement, uastParent: UElement?) : this(psi, uastParent, null)

    private val resolvedCall by lz {
        _resolvedCall ?: psi.getResolvedCall(psi.analyze())
    }

    override val receiverType by lz {
        val resolvedCall = this.resolvedCall ?: return@lz null
        val receiver = resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver ?: return@lz null
        receiver.type.toPsiType(this, psi, boxed = true)
    }

    override val methodName by lz { resolvedCall?.resultingDescriptor?.name?.asString() }

    override val classReference by lz {
        KotlinClassViaConstructorUSimpleReferenceExpression(psi, methodName.orAnonymous("class"), this)
    }

    override val methodIdentifier by lz {
        val calleeExpression = psi.calleeExpression ?: return@lz null
        KotlinUIdentifier(calleeExpression, this)
    }

    override val valueArgumentCount: Int
        get() = psi.valueArguments.size

    override val valueArguments by lz { psi.valueArguments.map { KotlinConverter.convertOrEmpty(it.getArgumentExpression(), this) } }

    override val typeArgumentCount: Int
        get() = psi.typeArguments.size

    override val typeArguments by lz { psi.typeArguments.map { it.typeReference.toPsiType(this, boxed = true) } }

    override val returnType: PsiType?
        get() = getExpressionType()

    override val kind: UastCallKind by lz {
        val resolvedCall = resolvedCall ?: return@lz UastCallKind.METHOD_CALL
        when {
            resolvedCall.resultingDescriptor is ConstructorDescriptor -> UastCallKind.CONSTRUCTOR_CALL
            this.isAnnotationArgumentArrayInitializer() -> UastCallKind.NESTED_ARRAY_INITIALIZER
            else -> UastCallKind.METHOD_CALL
        }
    }

    override val receiver: UExpression?
        get() = (uastParent as? UQualifiedReferenceExpression)?.takeIf { it.selector == this }?.receiver

    override fun resolve(): PsiMethod? {
        val descriptor = resolvedCall?.resultingDescriptor ?: return null
        val source = descriptor.toSource() ?: return null
        return resolveSource(descriptor, source)
    }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitCallExpression(this)) return
        methodIdentifier?.accept(visitor)
        classReference.accept(visitor)
        valueArguments.acceptList(visitor)

        visitor.afterVisitCallExpression(this)
    }

    private fun isAnnotationArgumentArrayInitializer(): Boolean {
        val resolvedCall = resolvedCall ?: return false
        // KtAnnotationEntry -> KtValueArgumentList -> KtValueArgument -> arrayOf call
        return psi.parents.elementAtOrNull(2) is KtAnnotationEntry && CompileTimeConstantUtils.isArrayFunctionCall(resolvedCall)
    }

    override fun convertParent(): UElement? = super.convertParent().let { result ->
        when (result) {
            is UMethod -> result.uastBody ?: result
            is UClass ->
                result.methods
                        .filterIsInstance<KotlinConstructorUMethod>()
                        .firstOrNull { it.isPrimary }
                        ?.uastBody
                ?: result
            else -> result
        }
    }

}