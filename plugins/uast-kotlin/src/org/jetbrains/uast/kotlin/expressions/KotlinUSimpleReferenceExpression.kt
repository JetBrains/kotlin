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

import com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.log
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier
import org.jetbrains.uast.kotlin.internal.DelegatedMultiResolve
import org.jetbrains.uast.visitor.UastVisitor

open class KotlinUSimpleReferenceExpression(
    override val sourcePsi: KtSimpleNameExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    private val resolvedDeclaration: PsiElement? by lz {
        sourcePsi.resolveCallToDeclaration()?.let { return@lz it }

        var declarationDescriptor = sourcePsi.analyze()[BindingContext.REFERENCE_TARGET, sourcePsi] ?: return@lz null
        if (declarationDescriptor is ImportedFromObjectCallableDescriptor<*>) {
            declarationDescriptor = declarationDescriptor.callableFromObject
        }
        if (declarationDescriptor is SyntheticJavaPropertyDescriptor) {
            declarationDescriptor = when (sourcePsi.readWriteAccess()) {
                ReferenceAccess.WRITE, ReferenceAccess.READ_WRITE ->
                    declarationDescriptor.setMethod ?: declarationDescriptor.getMethod
                ReferenceAccess.READ -> declarationDescriptor.getMethod
            }
        }

        if (declarationDescriptor is PackageViewDescriptor) {
            return@lz JavaPsiFacade.getInstance(sourcePsi.project).findPackage(declarationDescriptor.fqName.asString())
        }

        resolveToPsiClass(this, declarationDescriptor, sourcePsi)?.let { return@lz it }
        if (declarationDescriptor is DeclarationDescriptorWithSource) {
            declarationDescriptor.source.getPsi()?.let { it.getMaybeLightElement() ?: it }?.let { return@lz it }
        }
        return@lz resolveDeserialized(sourcePsi, declarationDescriptor, sourcePsi.readWriteAccess())
    }

    override val identifier get() = sourcePsi.getReferencedName()

    override fun resolve() = resolvedDeclaration

    override val resolvedName: String?
        get() = (resolvedDeclaration as? PsiNamedElement)?.name

    override fun accept(visitor: UastVisitor) {
        visitor.visitSimpleNameReferenceExpression(this)

        if (sourcePsi.parent.destructuringDeclarationInitializer != true) {
            visitAccessorCalls(visitor)
        }

        visitor.afterVisitSimpleNameReferenceExpression(this)
    }

    override val referenceNameElement: UElement? by lz { sourcePsi.getIdentifier()?.toUElement() }

    private fun visitAccessorCalls(visitor: UastVisitor) {
        // Visit Kotlin get-set synthetic Java property calls as function calls
        val bindingContext = sourcePsi.analyze()
        val access = sourcePsi.readWriteAccess()
        val resolvedCall = sourcePsi.getResolvedCall(bindingContext)
        val resultingDescriptor = resolvedCall?.resultingDescriptor as? SyntheticJavaPropertyDescriptor
        if (resultingDescriptor != null) {
            val setterValue = if (access.isWrite) {
                findAssignment(sourcePsi, sourcePsi.parent)?.right ?: run {
                    visitor.afterVisitSimpleNameReferenceExpression(this)
                    return
                }
            } else {
                null
            }

            if (access.isRead) {
                val getDescriptor = resultingDescriptor.getMethod
                KotlinAccessorCallExpression(sourcePsi, this, resolvedCall, getDescriptor, null).accept(visitor)
            }

            if (access.isWrite && setterValue != null) {
                val setDescriptor = resultingDescriptor.setMethod
                if (setDescriptor != null) {
                    KotlinAccessorCallExpression(sourcePsi, this, resolvedCall, setDescriptor, setterValue).accept(visitor)
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
        override val sourcePsi: KtSimpleNameExpression,
        override val uastParent: KotlinUSimpleReferenceExpression,
        private val resolvedCall: ResolvedCall<*>,
        private val accessorDescriptor: DeclarationDescriptor,
        val setterValue: KtExpression?
    ) : UCallExpressionEx, DelegatedMultiResolve, JvmDeclarationUElementPlaceholder {
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

        override val javaPsi: PsiElement? get() = null
        override val psi: PsiElement? get() = sourcePsi

        override val annotations: List<UAnnotation>
            get() = emptyList()

        override val receiverType by lz {
            val type = (resolvedCall.dispatchReceiver ?: resolvedCall.extensionReceiver)?.type ?: return@lz null
            type.toPsiType(this, sourcePsi, boxed = true)
        }

        override val methodIdentifier: UIdentifier? by lazy { KotlinUIdentifier(sourcePsi.getReferencedNameElement(), this) }

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

        override fun getArgumentForParameter(i: Int): UExpression? = valueArguments.getOrNull(i)

        override val typeArgumentCount: Int
            get() = resolvedCall.typeArguments.size

        override val typeArguments by lz {
            resolvedCall.typeArguments.values.map { it.toPsiType(this, sourcePsi, true) }
        }

        override val returnType by lz {
            (accessorDescriptor as? CallableDescriptor)?.returnType?.toPsiType(this, sourcePsi, boxed = false)
        }

        override val kind: UastCallKind
            get() = UastCallKind.METHOD_CALL

        override fun resolve(): PsiMethod? {
            val source = accessorDescriptor.toSource()
            return resolveSource(sourcePsi, accessorDescriptor, source)
        }
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
            in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) }
        )
            ReferenceAccess.READ_WRITE
        else
            ReferenceAccess.READ
    }
}

class KotlinClassViaConstructorUSimpleReferenceExpression(
    override val sourcePsi: KtCallElement,
    override val identifier: String,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression, KotlinUElementWithType {
    override val resolvedName: String?
        get() = (resolved as? PsiNamedElement)?.name

    private val resolved by lazy {
        when (val resultingDescriptor = sourcePsi.getResolvedCall(sourcePsi.analyze())?.descriptorForResolveViaConstructor()) {
            is ConstructorDescriptor -> {
                resultingDescriptor.constructedClass.toSource()?.getMaybeLightElement()
                    ?: (resultingDescriptor as? DeserializedCallableMemberDescriptor)?.let { resolveContainingDeserializedClass(sourcePsi, it) }
            }
            is SamConstructorDescriptor ->
                (resultingDescriptor.returnType?.getFunctionalInterfaceType(this, sourcePsi) as? PsiClassType)?.resolve()
            else -> null
        }
    }

    override fun resolve(): PsiElement? = resolved

    override fun asLogString(): String = log<USimpleNameReferenceExpression>("identifier = $identifier, resolvesTo = $resolvedName")

    // In new inference, SAM constructor is substituted with a function descriptor, so we use candidate descriptor to preserve behavior
    private fun ResolvedCall<*>.descriptorForResolveViaConstructor(): CallableDescriptor? {
        return if (this is NewResolvedCallImpl) candidateDescriptor else resultingDescriptor
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