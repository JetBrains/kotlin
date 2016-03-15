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

package org.jetbrains.kotlin.uast

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUFunctionCallExpression(
        override val psi: KtCallExpression,
        override val parent: UElement
) : UCallExpression, PsiElementBacked, KotlinTypeHelper, NoEvaluate {
    override val functionName: String?
        get() = (psi.calleeExpression as? KtSimpleNameExpression)?.getReferencedName()

    override val functionNameElement by lz { psi.calleeExpression?.let { KotlinConverter.convert(it, this) } }

    override val classReference by lz {
        KotlinClassViaConstructorUSimpleReferenceExpression(psi, functionName.orAnonymous("class"), this)
    }

    override val functionReference by lz {
        val calleeExpression = psi.calleeExpression ?: return@lz null
        val name = (calleeExpression as? KtSimpleNameExpression)?.getReferencedName() ?: return@lz null
        KotlinNameUSimpleReferenceExpression(calleeExpression, name, this)
    }

    override val valueArgumentCount: Int
        get() = psi.valueArguments.size

    override val valueArguments by lz { psi.valueArguments.map { KotlinConverter.convertOrEmpty(it.getArgumentExpression(), this) } }

    override val typeArgumentCount: Int
        get() = psi.typeArguments.size

    override val typeArguments by lz { psi.typeArguments.map { KotlinConverter.convert(it.typeReference, this) } }

    override val kind by lz {
        when (resolveCall()?.resultingDescriptor) {
            is ConstructorDescriptor -> UastCallKind.CONSTRUCTOR_CALL
            else -> UastCallKind.FUNCTION_CALL
        }
    }

    override fun resolve(context: UastContext): UFunction? {
        val resolvedCall = resolveCall()
        val descriptor = resolvedCall?.resultingDescriptor ?: return null
        val source = descriptor.toSource(psi) ?: return null

        if (descriptor is ConstructorDescriptor && descriptor.isPrimary
                && source is KtClassOrObject && source.getPrimaryConstructor() == null
                && source.getSecondaryConstructors().isEmpty()) {
            return (context.convert(source) as? UClass)?.constructors?.firstOrNull()
        }

        return context.convert(source) as? UFunction
    }

    private fun resolveCall() = psi.getResolvedCall(psi.analyze(BodyResolveMode.PARTIAL))
}

class KotlinUComponentFunctionCallExpression(
        override val psi: PsiElement,
        val n: Int,
        override val parent: UElement
) : UCallExpression, PsiElementBacked, NoEvaluate {
    override val valueArgumentCount = 0
    override val valueArguments = emptyList<UExpression>()
    override val typeArgumentCount = 0
    override val typeArguments = emptyList<UType>()
    override val classReference = null
    override val functionName = "component$n"
    override val functionReference by lz { KotlinStringUSimpleReferenceExpression(functionName, this) }
    override val functionNameElement = null
    override val kind = UastCallKind.FUNCTION_CALL
    override fun resolve(context: UastContext) = null
}