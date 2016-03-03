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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

abstract class KotlinAbstractUFunction : UFunction, PsiElementBacked {
    override abstract val psi: KtFunction

    override val name by lz { psi.name.orAnonymous() }

    override val valueParameterCount: Int
        get() = psi.valueParameters.size

    override val valueParameters by lz { psi.valueParameters.map { KotlinConverter.convert(it, this) } }

    override fun getSuperFunctions(context: UastContext): List<UFunction> {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val clazz = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, psi] as? FunctionDescriptor ?: return emptyList()
        return clazz.overriddenDescriptors.map {
            context.convert(DescriptorToSourceUtilsIde.getAnyDeclaration(psi.getProject(), it)) as? UFunction
        }.filterNotNull()
    }

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations by lz { psi.getUastAnnotations(this) }

    override val body by lz { KotlinConverter.convertOrEmpty(psi.bodyExpression, this) }
    override val visibility by lz { psi.getVisibility() }
}

class KotlinConstructorUFunction(
        override val psi: KtConstructor<*>,
        override val parent: UElement
) : KotlinAbstractUFunction(), PsiElementBacked {
    override val nameElement by lz {
        val constructorKeyword = psi.getConstructorKeyword()?.let { KotlinPsiElementStub(it, this) }
        constructorKeyword ?: this.getContainingFunction()?.nameElement
    }

    override val kind = UastFunctionKind.CONSTRUCTOR

    override val typeParameterCount = 0
    override val typeParameters = emptyList<UTypeReference>()

    override val returnType = null
}

class KotlinUFunction(
        override val psi: KtFunction,
        override val parent: UElement
) : KotlinAbstractUFunction(), PsiElementBacked {
    override val nameElement by lz { psi.nameIdentifier?.let { KotlinConverter.convert(it, this) } }

    override val kind = UastFunctionKind.FUNCTION

    override val returnType by lz {
        val descriptor = psi.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return@lz null
        val type = descriptor.returnType ?: return@lz null
        KotlinConverter.convert(type, psi.project, this)
    }

    //TODO
    override val typeParameterCount = 0
    override val typeParameters = emptyList<UTypeReference>()
}