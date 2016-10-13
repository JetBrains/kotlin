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

package org.jetbrains.uast.java

import com.intellij.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.expressions.UReferenceExpression
import org.jetbrains.uast.expressions.UTypeReferenceExpression
import org.jetbrains.uast.java.internal.JavaUElementWithComments
import org.jetbrains.uast.psi.PsiElementBacked

abstract class AbstractJavaUVariable : PsiVariable, UVariable, JavaUElementWithComments {
    override val uastInitializer by lz {
        val initializer = psi.initializer ?: return@lz null
        getLanguagePlugin().convertElement(initializer, this) as? UExpression
    }

    override val uastAnnotations by lz { psi.annotations.map { SimpleUAnnotation(it, this) } }
    override val typeReference by lz { getLanguagePlugin().convertOpt<UTypeReferenceExpression>(psi.typeElement, this) }

    override val uastAnchor: UElement
        get() = UIdentifier(psi.nameIdentifier, this)

    override fun equals(other: Any?) = this === other
    override fun hashCode() = psi.hashCode()
}

open class JavaUVariable(
        psi: PsiVariable,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), UVariable, PsiVariable by psi {
    override val psi = unwrap<UVariable, PsiVariable>(psi)
    
    companion object {
        fun create(psi: PsiVariable, containingElement: UElement?): UVariable {
            return when (psi) {
                is PsiEnumConstant -> JavaUEnumConstant(psi, containingElement)
                is PsiLocalVariable -> JavaULocalVariable(psi, containingElement)
                is PsiParameter -> JavaUParameter(psi, containingElement)
                is PsiField -> JavaUField(psi, containingElement)
                else -> JavaUVariable(psi, containingElement)
            }
        }
    }
}

open class JavaUParameter(
        psi: PsiParameter,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), UParameter, PsiParameter by psi {
    override val psi = unwrap<UParameter, PsiParameter>(psi)
}

open class JavaUField(
        psi: PsiField,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), UField, PsiField by psi {
    override val psi = unwrap<UField, PsiField>(psi)
}

open class JavaULocalVariable(
        psi: PsiLocalVariable,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), ULocalVariable, PsiLocalVariable by psi {
    override val psi = unwrap<ULocalVariable, PsiLocalVariable>(psi)
}

open class JavaUEnumConstant(
        psi: PsiEnumConstant,
        override val containingElement: UElement?
) : AbstractJavaUVariable(), UEnumConstant, PsiEnumConstant by psi {
    override val psi = unwrap<UEnumConstant, PsiEnumConstant>(psi)

    override val kind: UastCallKind
        get() = UastCallKind.CONSTRUCTOR_CALL
    override val receiver: UExpression?
        get() = null
    override val receiverType: PsiType?
        get() = null
    override val methodIdentifier: UIdentifier?
        get() = null
    override val classReference: UReferenceExpression?
        get() = JavaEnumConstantClassReference(psi, containingElement)
    override val typeArgumentCount: Int
        get() = 0
    override val typeArguments: List<PsiType>
        get() = emptyList()
    override val valueArgumentCount: Int
        get() = psi.argumentList?.expressions?.size ?: 0

    override val valueArguments by lz {
        psi.argumentList?.expressions?.map {
            getLanguagePlugin().convertElement(it, this) as? UExpression ?: UastEmptyExpression
        } ?: emptyList()
    }

    override val returnType: PsiType?
        get() = psi.type

    override fun resolve() = psi.resolveMethod()

    override val methodName: String?
        get() = null

    private class JavaEnumConstantClassReference(
            override val psi: PsiEnumConstant,
            override val containingElement: UElement?
    ) : JavaAbstractUExpression(), USimpleNameReferenceExpression, PsiElementBacked {
        override fun resolve() = psi.containingClass
        override val resolvedName: String?
            get() = psi.containingClass?.name
        override val identifier: String
            get() = psi.containingClass?.name ?: "<error>"
    }
}