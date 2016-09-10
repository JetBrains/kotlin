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
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.elements.LightParameter
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.internal.DefaultConstructorPsiMethod
import org.jetbrains.kotlin.java.model.types.JeNoneType
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

class JeTypeElement(override val psi: PsiClass) : JeElement, TypeElement, JeAnnotationOwner, JeModifierListOwner {
    override fun getEnclosingElement(): Element? {
        psi.containingClass?.let { return JeTypeElement(it) }
        val javaFile = psi.containingFile as? PsiJavaFile ?: return null
        return JavaPsiFacade.getInstance(psi.project).findPackage(javaFile.packageName)?.let(::JePackageElement)
    }
    
    override fun getSimpleName() = JeName(psi.name)

    override fun getQualifiedName() = JeName(psi.qualifiedName)

    private fun getSuperType(superTypes: Array<PsiClassType>, superClass: PsiClass): PsiClassType {
        return superTypes.firstOrNull { it is PsiClassReferenceType && it.resolve() == superClass }
               ?: PsiTypesUtil.getClassType(superClass)
    }
    
    override fun getSuperclass(): TypeMirror {
        val superClass = psi.superClass ?: return JeNoneType
        val psiType = getSuperType(psi.superTypes, superClass)
        return psiType.toJeType(psi.manager)
    }

    override fun getInterfaces(): List<TypeMirror> {
        val superTypes = psi.superTypes
        val interfaces = mutableListOf<TypeMirror>()
        
        for (intf in psi.interfaces) {
            val psiType = getSuperType(superTypes, intf)
            interfaces += psiType.toJeType(psi.manager)
        }
        
        return interfaces
    }

    override fun getTypeParameters() = psi.typeParameters.map { JeTypeParameterElement(it, this) }

    override fun getNestingKind() = when {
        ClassUtil.isTopLevelClass(psi) -> NestingKind.TOP_LEVEL
        psi.parent is PsiClass -> NestingKind.MEMBER
        psi is PsiAnonymousClass -> NestingKind.ANONYMOUS
        else -> NestingKind.LOCAL
    }

    override fun getEnclosedElements(): List<JeElement> {
        val declarations = mutableListOf<JeElement>()
        psi.initializers.forEach { declarations += JeClassInitializerExecutableElement(it) }
        psi.fields.forEach { declarations += JeVariableElement(it) }
        psi.methods.forEach { declarations += JeMethodExecutableElement(it) }
        psi.innerClasses.forEach { declarations += JeTypeElement(it) }
        
        // Add default constructor if possible
        if (!psi.isInterface && !psi.isAnnotationType && psi.constructors.isEmpty()) {
            val superClass = psi.superClass
            val canHaveDefaultConstructor = superClass == null || run {
                val constructors = superClass.constructors
                constructors.isEmpty() || constructors.any {
                    (it.hasModifierProperty(PsiModifier.PUBLIC) || it.hasModifierProperty(PsiModifier.PROTECTED) || run {
                        it.hasModifierProperty(PsiModifier.PACKAGE_LOCAL) && psi.packageName == superClass.packageName
                    }) && it.parameterList.parametersCount == 0 
                }
            }
            
            if (canHaveDefaultConstructor) {
                declarations += JeMethodExecutableElement(DefaultConstructorPsiMethod(psi, psi.language).apply {
                    val containingClass = psi.containingClass
                    if (containingClass != null && !psi.hasModifierProperty(PsiModifier.STATIC)) {
                        addParameter(LightParameter("\$instance", PsiTypesUtil.getClassType(containingClass), this, psi.language))
                    }
                })
            }
        }
        
        return declarations
    }
    
    private val PsiClass.packageName: String
        get() = (containingFile as? PsiJavaFile)?.packageName ?: ""
    
    fun getAllMembers(): List<Element> {
        val declarations = mutableListOf<Element>()
        psi.allFields.forEach { declarations += JeVariableElement(it) }
        psi.allMethods.forEach {
            if (it.isConstructor && it.containingClass != this@JeTypeElement.psi) return@forEach
            declarations += JeMethodExecutableElement(it) 
        }
        psi.allInnerClasses.forEach { declarations += JeTypeElement(it) }
        psi.initializers.forEach { declarations += JeClassInitializerExecutableElement(it) }
        return declarations
    }

    override fun getKind() = when {
        psi.isEnum -> ElementKind.ENUM
        psi.isAnnotationType -> ElementKind.ANNOTATION_TYPE
        psi.isInterface -> ElementKind.INTERFACE
        else -> ElementKind.CLASS
    }

    override fun asType() = PsiTypesUtil.getClassType(psi).toJeType(psi.manager)

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitType(this, p)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return psi == (other as JeTypeElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.qualifiedName ?: "<anonymous> extends " + psi.superClass?.qualifiedName ?: "<unnamed>"
}