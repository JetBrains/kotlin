/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.LightClassInheritanceHelper
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightMethod
import org.jetbrains.kotlin.idea.caches.lightClasses.KtLightClassForDecompiledDeclaration
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils

class LightClassProviderImpl : LightClassProvider {
    override fun getLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField? =
        LightClassUtil.getLightFieldForCompanionObject(companionObject)

    override fun getLightClassMethods(function: KtFunction): List<PsiMethod> =
        LightClassUtil.getLightClassMethods(function)

    override fun getLightClassParameterDeclarations(parameter: KtParameter): List<PsiNamedElement> =
        LightClassUtil.getLightClassPropertyMethods(parameter).allDeclarations

    override fun getLightClassPropertyDeclarations(property: KtProperty): List<PsiNamedElement> =
        LightClassUtil.getLightClassPropertyMethods(property).allDeclarations

    override fun toLightClassWithBuiltinMapping(classOrObject: KtClassOrObject): PsiClass? =
        classOrObject.toLightClassWithBuiltinMapping()

    override fun toLightMethods(psiElement: PsiElement): List<PsiMethod> =
        psiElement.toLightMethods()

    override fun toLightClass(classOrObject: KtClassOrObject): KtLightClass? =
        classOrObject.toLightClass()

    override fun toLightElements(ktElement: KtElement): List<PsiNamedElement> =
        ktElement.toLightElements()

    override fun createKtFakeLightClass(kotlinOrigin: KtClassOrObject): PsiClass? =
        KtFakeLightClass(kotlinOrigin)

    override fun getRepresentativeLightMethod(psiElement: PsiElement): PsiMethod? =
        psiElement.getRepresentativeLightMethod()

    override fun isKtFakeLightClass(psiClass: PsiClass): Boolean =
        psiClass is KtFakeLightClass

    override fun isKtLightClassForDecompiledDeclaration(psiClass: PsiClass): Boolean =
        psiClass is KtLightClassForDecompiledDeclaration

    override fun createKtFakeLightMethod(ktDeclaration: KtNamedDeclaration): PsiMethod? =
        KtFakeLightMethod.get(ktDeclaration)

    override fun isFakeLightClassInheritor(ktFakeLightClass: KtFakeLightClass, baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (ktFakeLightClass.manager.areElementsEquivalent(baseClass, ktFakeLightClass)) return false
        LightClassInheritanceHelper.getService(ktFakeLightClass.project).isInheritor(ktFakeLightClass, baseClass, checkDeep).ifSure { return it }

        val baseKtClass = (baseClass as? KtLightClass)?.kotlinOrigin ?: return false
        val baseDescriptor = baseKtClass.resolveToDescriptorIfAny() ?: return false
        val thisDescriptor = ktFakeLightClass.kotlinOrigin.resolveToDescriptorIfAny() ?: return false

        val thisFqName = DescriptorUtils.getFqName(thisDescriptor).asString()
        val baseFqName = DescriptorUtils.getFqName(baseDescriptor).asString()
        if (thisFqName == baseFqName) return false

        return if (checkDeep)
            DescriptorUtils.isSubclass(thisDescriptor, baseDescriptor)
        else
            DescriptorUtils.isDirectSubclass(thisDescriptor, baseDescriptor)
    }
}