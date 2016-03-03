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
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked
import java.util.*

class JavaUClass(
        override val psi: PsiClass,
        override val parent: UElement?,
        val newExpression: PsiNewExpression? = null
) : UClass, PsiElementBacked {
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

    override val isEnum: Boolean
        get() = psi.isEnum

    override val isInterface: Boolean
        get() = psi.isInterface

    override val isAnnotation: Boolean
        get() = psi.isAnnotationType

    override val isObject = psi is PsiAnonymousClass
    override val isAnonymous = psi is PsiAnonymousClass

    override val internalName = null

    override val superTypes by lz {
        psi.extendsListTypes.map { JavaConverter.convert(it, this) } + psi.implementsListTypes.map { JavaConverter.convert(it, this) }
    }

    override fun getSuperClass(context: UastContext) = context.convert(psi.superClass) as? UClass

    override val visibility: UastVisibility
        get() = psi.getVisibility()

    override fun hasModifier(modifier: UastModifier) = when (modifier) {
        UastModifier.INNER -> !psi.hasModifierProperty(PsiModifier.STATIC) && !isTopLevel()
        else -> psi.hasModifier(modifier)
    }

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
) : UFunction, PsiElementBacked, NoAnnotations, NoModifiers {
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

    override val returnType = null
    override val body = EmptyExpression(this)
    override val visibility = UastVisibility.LOCAL

    override fun getSuperFunctions(context: UastContext) = emptyList<UFunction>()

    override val nameElement = null
    override val name = "<init>"
}

private class JavaUAnonymousClassConstructorParameter(
        val psi: PsiExpressionList,
        val index: Int,
        override val parent: UElement
) : UVariable, NoAnnotations, NoModifiers {
    override val initializer by lz { JavaConverter.convert(psi.expressions[index], this) }
    override val kind = UastVariableKind.VALUE_PARAMETER
    override val type by lz { JavaConverter.convert(psi.expressionTypes[index], this) }
    override val nameElement = null
    override val name = "p$index"
}