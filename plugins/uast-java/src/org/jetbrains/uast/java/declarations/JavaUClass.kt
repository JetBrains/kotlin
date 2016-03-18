/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.*
import org.jetbrains.uast.kinds.UastClassKind
import org.jetbrains.uast.psi.PsiElementBacked
import java.util.*

class JavaUClass(
        override val psi: PsiClass,
        override val parent: UElement?,
        val newExpression: PsiNewExpression? = null
) : JavaAbstractUElement(), UClass, PsiElementBacked {
    override val name: String
        get() = psi.name.orAnonymous()

    override val nameElement by lz {
        if (psi is PsiAnonymousClass && newExpression != null) {
            newExpression.classOrAnonymousClassReference?.referenceNameElement?.let { JavaPsiElementStub(it, this) }
        } else {
            JavaConverter.convert(psi.nameIdentifier, this)
        }
    }

    override val fqName: String?
        get() = psi.qualifiedName

    override val kind by lz {
        when {
            psi.isEnum -> UastClassKind.ENUM
            psi.isAnnotationType -> UastClassKind.ANNOTATION
            psi.isInterface -> UastClassKind.INTERFACE
            psi is PsiAnonymousClass -> UastClassKind.OBJECT
            else -> UastClassKind.CLASS
        }
    }

    override val defaultType by lz { JavaConverter.convert(PsiTypesUtil.getClassType(psi), this) }

    override val companions: List<UClass>
        get() = emptyList()

    override val isAnonymous: Boolean
        get() = psi is PsiAnonymousClass

    override val internalName = null

    override val superTypes by lz {
        psi.extendsListTypes.map { JavaConverter.convert(it, this) } + psi.implementsListTypes.map { JavaConverter.convert(it, this) }
    }

    override fun getSuperClass(context: UastContext) = context.convert(psi.superClass) as? UClass

    override val visibility: UastVisibility
        get() = psi.getVisibility()

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations by lz { psi.modifierList.getAnnotations(this) }

    override val declarations by lz {
        val declarations = arrayListOf<UDeclaration>()
        psi.fields.mapTo(declarations) { JavaConverter.convert(it, this) }
        psi.constructors.mapTo(declarations) { JavaConverter.convert(it, this) }

        if (psi is PsiAnonymousClass && newExpression != null) {
            declarations += JavaUAnonymousClassConstructor(psi, newExpression, this)
        }

        psi.methods.filter { !it.isConstructor }.mapTo(declarations) { JavaConverter.convert(it, this) }
        psi.interfaces.mapTo(declarations) { JavaConverter.convert(it, this) }
        psi.innerClasses.mapTo(declarations) { JavaConverter.convert(it, this) }
        psi.initializers.mapTo(declarations) { JavaConverter.convert(it, this) }
        declarations
    }

    override fun isSubclassOf(name: String): Boolean {
        tailrec fun isSubClassOf(clazz: PsiClass?, name: String): Boolean = when {
            clazz == null -> false
            clazz.qualifiedName == name -> true
            else -> isSubClassOf(clazz.superClass, name)
        }

        return isSubClassOf(psi, name)
    }
}

private class JavaUAnonymousClassConstructor(
        override val psi: PsiAnonymousClass,
        val newExpression: PsiNewExpression,
        override val parent: UElement
) : JavaAbstractUElement(), UFunction, PsiElementBacked, NoAnnotations, NoModifiers {
    override val kind = UastFunctionKind.CONSTRUCTOR

    override val valueParameterCount by lz { newExpression.argumentList?.expressions?.size ?: 0 }

    override val valueParameters by lz {
        val args = newExpression.argumentList ?: return@lz emptyList<UVariable>()
        val variables = ArrayList<UVariable>(args.expressions.size)

        for (i in 0..(args.expressions.size - 1)) {
            variables += JavaUAnonymousClassConstructorParameter(args, i, this)
        }

        variables
    }
    override val typeParameters by lz { psi.typeParameters.map { JavaConverter.convert(it, this) } }

    override val typeParameterCount: Int
        get() = psi.typeParameters.size

    override val returnType: UType?
        get() = null

    override val body = EmptyExpression(this)

    override val visibility: UastVisibility
        get() = UastVisibility.LOCAL

    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()

    override val nameElement: UElement?
        get() = null

    override val name: String
        get() = "<init>"
}

private class JavaUAnonymousClassConstructorParameter(
        val psi: PsiExpressionList,
        val index: Int,
        override val parent: UElement
) : JavaAbstractUElement(), UVariable, NoAnnotations, NoModifiers {
    override val initializer by lz { JavaConverter.convert(psi.expressions[index], this) }

    override val kind: UastVariableKind
        get() = UastVariableKind.VALUE_PARAMETER

    override val type by lz { JavaConverter.convert(psi.expressionTypes[index], this) }

    override val nameElement: UElement?
        get() = null

    override val name: String
        get() = "p$index"
}