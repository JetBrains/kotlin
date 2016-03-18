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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

open class KotlinUVariable(
        override val psi: KtVariableDeclaration,
        override val parent: UElement
) : KotlinAbstractUElement(), UVariable, PsiElementBacked {
    override val name: String
        get() = psi.name.orAnonymous()

    override val nameElement by lz { KotlinDumbUElement(psi.nameIdentifier, this) }

    override val initializer by lz { KotlinConverter.convertOrEmpty(psi.initializer, this) }

    override val type by lz {
        val descriptor = psi.resolveToDescriptorIfAny() as? CallableDescriptor ?: return@lz UastErrorType
        val type = descriptor.returnType ?: return@lz UastErrorType
        KotlinConverter.convert(type, psi.project, this)
    }

    override val kind: UastVariableKind
        get() = when (psi.parent) {
            is KtClassBody -> UastVariableKind.MEMBER
            is KtClassOrObject -> UastVariableKind.MEMBER
            else -> UastVariableKind.LOCAL_VARIABLE
        }

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations by lz { psi.getUastAnnotations(this) }
}

class KotlinDestructuredUVariable(
        val entry: KtDestructuringDeclarationEntry,
        parent: UElement
) : KotlinUVariable(entry, parent) {
    override lateinit var initializer: UExpression
        internal set

    override val type by lz {
        val bindingContext = entry.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = bindingContext[BindingContext.COMPONENT_RESOLVED_CALL, entry] ?: return@lz UastErrorType
        val returnType = resolvedCall.resultingDescriptor.returnType ?: return@lz UastErrorType
        KotlinConverter.convert(returnType, entry.project, this)
    }
}

class KotlinDestructuringUVariable(
        override val psi: KtDestructuringDeclaration,
        override val parent: UElement
) : KotlinAbstractUElement(), UVariable, PsiElementBacked {
    override val name = "var" + psi.text.hashCode()
    override val initializer by lz { KotlinConverter.convertOrEmpty(psi.initializer, this) }
    override val kind = UastVariableKind.LOCAL_VARIABLE
    override val type: UType
        get() = initializer.getExpressionType() ?: UastErrorType

    override val nameElement = null
    override fun hasModifier(modifier: UastModifier) = false
    override val annotations = emptyList<UAnnotation>()
}

class KotlinParameterUVariable(
        override val psi: KtParameter,
        override val parent: UElement
) : KotlinAbstractUElement(), UVariable, PsiElementBacked {
    override val name: String
        get() = psi.name.orAnonymous()

    override val nameElement by lz { KotlinDumbUElement(psi.nameIdentifier, this) }

    override val initializer by lz { KotlinConverter.convert(psi.defaultValue, this) as? UExpression }

    override val kind = UastVariableKind.VALUE_PARAMETER

    override val type by lz {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val param = bindingContext[BindingContext.VALUE_PARAMETER, psi] ?: return@lz UastErrorType
        KotlinConverter.convert(param.type, psi.project, this)
    }

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations = psi.getUastAnnotations(this)
}