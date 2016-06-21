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

package org.jetbrains.kotlin.java.model.elements

import com.intellij.psi.*
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.types.JeNoneType
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

class JeTypeElement(override val psi: PsiClass) : JeElement(), TypeElement, JeAnnotationOwner, JeModifierListOwner {
    override fun getEnclosingElement(): Element? {
        psi.containingClass?.let { return JeTypeElement(it) }
        val javaFile = psi.containingFile as? PsiJavaFile ?: return null
        return JavaPsiFacade.getInstance(psi.project).findPackage(javaFile.packageName)?.let { JePackageElement(it) }
    }
    
    override val annotationOwner: PsiAnnotationOwner?
        get() = psi.modifierList

    override fun getSimpleName() = JeName(psi.name)

    override fun getQualifiedName() = JeName(psi.qualifiedName)

    override fun getSuperclass(): TypeMirror {
        val superClass = psi.superClass ?: return JeNoneType
        return PsiTypesUtil.getClassType(superClass).toJeType()
    }

    override fun getInterfaces() = psi.interfaces.map { PsiTypesUtil.getClassType(it).toJeType() }

    override fun getTypeParameters() = psi.typeParameters.map { JeTypeParameterElement(it, this) }

    override fun getNestingKind() = when {
        ClassUtil.isTopLevelClass(psi) -> NestingKind.TOP_LEVEL
        psi.parent is PsiClass -> NestingKind.MEMBER
        psi is PsiAnonymousClass -> NestingKind.ANONYMOUS
        else -> NestingKind.LOCAL
    }

    override fun getEnclosedElements(): List<Element> {
        val declarations = mutableListOf<Element>()
        psi.initializers.forEach { declarations += JeClassInitializerExecutableElement(it) }
        psi.fields.forEach { declarations += JeVariableElement(it) }
        psi.methods.forEach { declarations += JeMethodExecutableElement(it) }
        psi.innerClasses.forEach { declarations += JeTypeElement(it) }
        return declarations
    }
    
    fun getAllMembers(): List<Element> {
        val declarations = mutableListOf<Element>()
        psi.constructors.forEach { declarations += JeMethodExecutableElement(it) }
        psi.fields.forEach { declarations += JeVariableElement(it) }
        psi.methods.forEach { declarations += JeMethodExecutableElement(it) }
        return declarations
    }

    override fun getKind() = when {
        psi.isEnum -> ElementKind.ENUM
        psi.isAnnotationType -> ElementKind.ANNOTATION_TYPE
        psi.isInterface -> ElementKind.INTERFACE
        else -> ElementKind.CLASS
    }

    override fun asType() = PsiTypesUtil.getClassType(psi).toJeType()

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitType(this, p)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return psi == (other as JeTypeElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.qualifiedName ?: psi.superClass?.qualifiedName ?: "<unnamed>"
}