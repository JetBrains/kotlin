/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.*

class LightClassProviderFirImpl : LightClassProvider {
    override fun getLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField? {
        return null
    }

    override fun getLightClassMethods(function: KtFunction): List<PsiMethod> {
        return emptyList()
    }

    override fun getLightClassParameterDeclarations(parameter: KtParameter): List<PsiNamedElement> {
        return emptyList()
    }

    override fun getLightClassPropertyDeclarations(property: KtProperty): List<PsiNamedElement> {
        return emptyList()
    }

    override fun toLightClassWithBuiltinMapping(classOrObject: KtClassOrObject): PsiClass? {
        return null
    }

    override fun toLightMethods(psiElement: PsiElement): List<PsiMethod> {
        return emptyList()
    }

    override fun toLightClass(classOrObject: KtClassOrObject): KtLightClass? {
        return null
    }

    override fun toLightElements(ktElement: KtElement): List<PsiNamedElement> {
        return emptyList()
    }

    override fun createKtFakeLightClass(kotlinOrigin: KtClassOrObject): PsiClass? {
        return null
    }

    override fun getRepresentativeLightMethod(psiElement: PsiElement): PsiMethod? {
        return null
    }

    override fun isKtFakeLightClass(psiClass: PsiClass): Boolean {
        return false
    }

    override fun isKtLightClassForDecompiledDeclaration(psiClass: PsiClass): Boolean {
        return false
    }

    override fun createKtFakeLightMethod(ktDeclaration: KtNamedDeclaration): PsiMethod? {
        return null
    }
}