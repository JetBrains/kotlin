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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUTypeReference(
        override val psi: KtTypeReference,
        override val parent: UElement
) : KotlinAbstractUElement(), UTypeReference, PsiElementBacked {
    override fun resolve(context: UastContext): UClass? {
        val descriptor = psi.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, psi]
                                 ?.constructor?.declarationDescriptor as? ClassDescriptor ?: return null
        return context.convert(descriptor.toSource()) as? UClass
    }

    override val nameElement: UElement?
        get() = KotlinDumbUElement(psi, this)

    override val name: String
        get() = psi.name.orAnonymous()
}