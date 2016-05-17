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
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UObjectLiteralExpression
import org.jetbrains.uast.UType
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUObjectLiteralExpression(
        override val psi: KtObjectLiteralExpression,
        override val parent: UElement
) : UObjectLiteralExpression, PsiElementBacked, KotlinUElementWithType {
    override val declaration by lz { KotlinUClass(psi.objectDeclaration, this, true) }
    override fun getExpressionType(): UType? {
        val obj = psi.objectDeclaration
        val bindingContext = obj.analyze(BodyResolveMode.PARTIAL)
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, obj] as? ClassDescriptor ?: return null
        return KotlinConverter.convert(descriptor.getSuperClassOrAny().defaultType, psi.project, null)
    }
}