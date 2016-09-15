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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUAnnotation(
        override val psi: KtAnnotationEntry,
        override val parent: UElement
) : KotlinAbstractUElement(), UAnnotation, PsiElementBacked {
    override val fqName: String?
        get() = resolveToDescriptor()?.fqNameSafe?.asString()

    override val name: String
        get() = (psi.typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

    override val valueArguments by lz {
        psi.valueArguments.map {
            val name = it.getArgumentName()?.asName?.identifier.orAnonymous()
            UNamedExpression(name, this).apply {
                expression = KotlinConverter.convertOrEmpty(it.getArgumentExpression(), this)
            }
        }
    }

    private fun resolveToDescriptor(): ClassDescriptor? {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = psi.calleeExpression?.getResolvedCall(bindingContext) ?: return null
        return (resolvedCall.resultingDescriptor as? ClassConstructorDescriptor)?.containingDeclaration
    }

    override fun resolve(context: UastContext): UClass? {
        val classDescriptor = resolveToDescriptor() ?: return null
        val source = classDescriptor.toSource() ?: return null
        return context.convert(source) as? UClass
    }
}