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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

open class KotlinUSimpleReferenceExpression(
        override val psi: PsiElement,
        override val identifier: String,
        override val parent: UElement
) : USimpleReferenceExpression, PsiElementBacked, KotlinTypeHelper, KotlinEvaluateHelper {
    override fun resolve(context: UastContext) = context.convert(
            psi.references.firstOrNull()?.resolve()) as? UDeclaration
}

class KotlinNameUSimpleReferenceExpression(
        psi: PsiElement,
        identifier: String,
        parent: UElement
) : KotlinUSimpleReferenceExpression(psi, identifier, parent)

class KotlinClassViaConstructorUSimpleReferenceExpression(
        override val psi: KtCallExpression,
        override val identifier: String,
        override val parent: UElement
) : USimpleReferenceExpression, PsiElementBacked, KotlinTypeHelper, NoEvaluate {
    override fun resolve(context: UastContext): UDeclaration? {
        val resolvedCall = psi.getResolvedCall(psi.analyze(BodyResolveMode.PARTIAL))
        val resultingDescriptor = resolvedCall?.resultingDescriptor as? ConstructorDescriptor ?: return null
        val clazz = resultingDescriptor.containingDeclaration
        val source = DescriptorToSourceUtilsIde.getAnyDeclaration(psi.project, clazz) ?: return null
        return context.convert(source) as? UClass
    }
}

class KotlinStringUSimpleReferenceExpression(
        override val identifier: String,
        override val parent: UElement
) : USimpleReferenceExpression, NoEvaluate {
    override fun resolve(context: UastContext) = null
}