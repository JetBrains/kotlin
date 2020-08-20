/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.light.AbstractLightClass
import com.intellij.psi.impl.light.LightMethod
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

interface LightClassProvider {

    fun getLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField?

    fun getLightClassMethods(function: KtFunction): List<PsiMethod>

    //getLightClassPropertyMethods.allDeclarations
    fun getLightClassParameterDeclarations(parameter: KtParameter): List<PsiNamedElement>

    //getLightClassPropertyMethods.allDeclarations
    fun getLightClassPropertyDeclarations(property: KtProperty): List<PsiNamedElement>

    fun toLightClassWithBuiltinMapping(classOrObject: KtClassOrObject): PsiClass?

    fun toLightMethods(psiElement: PsiElement): List<PsiMethod>

    fun toLightClass(classOrObject: KtClassOrObject): KtLightClass?

    fun toLightElements(ktElement: KtElement): List<PsiNamedElement>

    fun createKtFakeLightClass(kotlinOrigin: KtClassOrObject): PsiClass

    fun getRepresentativeLightMethod(psiElement: PsiElement): PsiMethod?

    fun isKtFakeLightClass(psiClass: PsiClass): Boolean

    fun createKtFakeLightMethod(ktDeclaration: KtNamedDeclaration): PsiMethod?

    companion object {

        fun getInstance(project: Project): LightClassProvider {
            return ServiceManager.getService(project, LightClassProvider::class.java)
        }

        fun providedGetLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField? =
            getInstance(companionObject.project).getLightFieldForCompanionObject(companionObject)

        fun providedGetLightClassMethods(function: KtFunction): List<PsiMethod> =
            getInstance(function.project).getLightClassMethods(function)

        //getLightClassPropertyMethods.allDeclarations
        fun providedGetLightClassParameterDeclarations(parameter: KtParameter): List<PsiNamedElement> =
            getInstance(parameter.project).getLightClassParameterDeclarations(parameter)

        //getLightClassPropertyMethods.allDeclarations
        fun providedGetLightClassPropertyDeclarations(property: KtProperty): List<PsiNamedElement> =
            getInstance(property.project).getLightClassPropertyDeclarations(property)

        fun KtClassOrObject.providedToLightClassWithBuiltinMapping(): PsiClass? =
            getInstance(project).toLightClassWithBuiltinMapping(this)

        fun PsiElement.providedToLightMethods(): List<PsiMethod> =
            getInstance(project).toLightMethods(this)

        fun KtClassOrObject.providedToLightClass(): KtLightClass? =
            getInstance(project).toLightClass(this)

        fun KtElement.providedToLightElements(): List<PsiNamedElement> =
            getInstance(project).toLightElements(this)

        fun providedCreateKtFakeLightClass(kotlinOrigin: KtClassOrObject): PsiClass? =
            getInstance(kotlinOrigin.project).createKtFakeLightClass(kotlinOrigin)

        fun PsiClass.providedIsKtFakeLightClass(): Boolean =
            getInstance(project).isKtFakeLightClass(this)

        fun providedCreateKtFakeLightMethod(ktDeclaration: KtNamedDeclaration): PsiMethod? =
            getInstance(ktDeclaration.project).createKtFakeLightMethod(ktDeclaration)

        fun PsiElement.providedGetRepresentativeLightMethod(): PsiMethod? =
            getInstance(project).getRepresentativeLightMethod(this)
    }
}