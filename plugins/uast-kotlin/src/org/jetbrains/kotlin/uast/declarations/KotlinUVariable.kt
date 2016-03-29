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

    override val getters: List<UFunction>? by lz {
        (psi as? KtProperty)?.accessors?.filter { it.isGetter }?.map { VariableAccessorFunction(it, this) }
    }

    override val setters: List<UFunction>? by lz {
        (psi as? KtProperty)?.accessors?.filter { it.isSetter }?.map { VariableAccessorFunction(it, this) }
    }

    override val kind: UastVariableKind
        get() = when (psi.parent) {
            is KtClassBody -> UastVariableKind.MEMBER
            is KtClassOrObject -> UastVariableKind.MEMBER
            else -> UastVariableKind.LOCAL_VARIABLE
        }

    override val visibility: UastVisibility
        get() = psi.getVisibility()

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations by lz { psi.getUastAnnotations(this) }

    private class VariableAccessorFunction(
            override val psi: KtPropertyAccessor,
            override val parent: UVariable
    ) : KotlinAbstractUElement(), UFunction, PsiElementBacked {
        override val kind: UastFunctionKind
            get() = if (psi.isGetter)
                UastFunctionKind.GETTER
            else if (psi.isSetter)
                UastFunctionKind.SETTER
            else
                UastFunctionKind.FUNCTION

        override val valueParameters by lz { psi.valueParameters.map { KotlinConverter.convert(it, this) } }

        override val valueParameterCount: Int
            get() = psi.valueParameters.size

        override val typeParameters: List<UTypeReference>
            get() = emptyList()
        override val typeParameterCount: Int
            get() = 0

        override val returnType: UType?
            get() = if (psi.isSetter) null else parent.type

        override val body by lz { KotlinConverter.convertOrEmpty(psi.bodyExpression, this) }

        override val visibility: UastVisibility
            get() = psi.getVisibility()

        override fun getSuperFunctions(context: UastContext): List<UFunction> {
            throw UnsupportedOperationException()
        }

        override val nameElement: UElement?
            get() = throw UnsupportedOperationException()
        override val name: String
            get() = throw UnsupportedOperationException()

        override fun hasModifier(modifier: UastModifier): Boolean {
            throw UnsupportedOperationException()
        }

        override val annotations: List<UAnnotation>
            get() = throw UnsupportedOperationException()
    }
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

    override val visibility: UastVisibility
        get() = UastVisibility.LOCAL
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

    override val visibility: UastVisibility
        get() = UastVisibility.LOCAL
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

    override val visibility: UastVisibility
        get() = UastVisibility.LOCAL

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations = psi.getUastAnnotations(this)
}