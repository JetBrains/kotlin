/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath

class KotlinSearchUsagesSupportFirImpl : KotlinSearchUsagesSupport {
    override fun dataClassComponentMethodName(element: KtParameter): String? {
        return null
    }

    override fun hasType(element: KtExpression): Boolean {
        return false
    }

    override fun isSamInterface(psiClass: PsiClass): Boolean {
        return false
    }

    override fun <T : PsiNamedElement> filterDataClassComponentsIfDisabled(
        elements: List<T>,
        kotlinOptions: KotlinReferencesSearchOptions
    ): List<T> {
        return emptyList()
    }

    override fun isCallableOverrideUsage(reference: PsiReference, declaration: KtNamedDeclaration): Boolean {
        return false
    }

    override fun isUsageInContainingDeclaration(reference: PsiReference, declaration: KtNamedDeclaration): Boolean {
        return false
    }

    override fun isExtensionOfDeclarationClassUsage(reference: PsiReference, declaration: KtNamedDeclaration): Boolean {
        return false
    }

    override fun getReceiverTypeSearcherInfo(psiElement: PsiElement, isDestructionDeclarationSearch: Boolean): ReceiverTypeSearcherInfo? {
        return null
    }

    override fun forceResolveReferences(file: KtFile, elements: List<KtElement>) {

    }

    override fun scriptDefinitionExists(file: PsiFile): Boolean {
        return false
    }

    override fun getDefaultImports(file: KtFile): List<ImportPath> {
        return emptyList()
    }

    override fun forEachKotlinOverride(
        ktClass: KtClass,
        members: List<KtNamedDeclaration>,
        scope: SearchScope,
        processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
    ): Boolean {
        return false
    }

    override fun forEachOverridingMethod(method: PsiMethod, scope: SearchScope, processor: (PsiMethod) -> Boolean): Boolean {
        return false
    }

    override fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement> {
        return emptyList()
    }

    override fun findTypeAliasByShortName(shortName: String, project: Project, scope: GlobalSearchScope): Collection<KtTypeAlias> {
        return emptyList()
    }

    override fun isInProjectSource(element: PsiElement, includeScriptsOutsideSourceRoots: Boolean): Boolean {
        return true
    }

    override fun isOverridable(declaration: KtDeclaration): Boolean {
        return false
    }

    override fun isInheritable(ktClass: KtClass): Boolean {
        return false
    }

    override fun formatJavaOrLightMethod(method: PsiMethod): String {
        return "FORMAT JAVA OR LIGHT METHOD ${method.name}"
    }

    override fun formatClass(classOrObject: KtClassOrObject): String {
        return "FORMAT CLASS ${classOrObject.name}"
    }

    override fun expectedDeclarationIfAny(declaration: KtDeclaration): KtDeclaration? {
        return null
    }

    override fun isExpectDeclaration(declaration: KtDeclaration): Boolean {
        return false
    }
}