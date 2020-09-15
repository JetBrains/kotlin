/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath

interface KotlinSearchUsagesSupport {

    interface ConstructorCallHandle {
        fun referencedTo(element: KtElement): Boolean
    }

    companion object {
        fun getInstance(project: Project): KotlinSearchUsagesSupport {
            return ServiceManager.getService(project, KotlinSearchUsagesSupport::class.java)
        }

        val KtParameter.dataClassComponentMethodName: String?
            get() = getInstance(project).dataClassComponentMethodName(this)

        val KtExpression.hasType: Boolean
            get() = getInstance(project).hasType(this)

        val PsiClass.isSamInterface: Boolean
            get() = getInstance(project).isSamInterface(this)

        fun <T : PsiNamedElement> List<T>.filterDataClassComponentsIfDisabled(kotlinOptions: KotlinReferencesSearchOptions): List<T> =
            firstOrNull()?.let {
                getInstance(it.project).filterDataClassComponentsIfDisabled(this, kotlinOptions)
            } ?: this

        fun PsiReference.isCallableOverrideUsage(declaration: KtNamedDeclaration): Boolean =
            getInstance(declaration.project).isCallableOverrideUsage(this, declaration)

        fun PsiReference.isUsageInContainingDeclaration(declaration: KtNamedDeclaration): Boolean =
            getInstance(declaration.project).isUsageInContainingDeclaration(this, declaration)

        fun PsiReference.isExtensionOfDeclarationClassUsage(declaration: KtNamedDeclaration): Boolean =
            getInstance(declaration.project).isExtensionOfDeclarationClassUsage(this, declaration)

        fun PsiElement.getReceiverTypeSearcherInfo(isDestructionDeclarationSearch: Boolean): ReceiverTypeSearcherInfo? =
            getInstance(project).getReceiverTypeSearcherInfo(this, isDestructionDeclarationSearch)

        fun KtFile.forceResolveReferences(elements: List<KtElement>) =
            getInstance(project).forceResolveReferences(this, elements)

        fun PsiFile.scriptDefinitionExists(): Boolean =
            getInstance(project).scriptDefinitionExists(this)

        fun KtFile.getDefaultImports(): List<ImportPath> =
            getInstance(project).getDefaultImports(this)

        fun forEachKotlinOverride(
            ktClass: KtClass,
            members: List<KtNamedDeclaration>,
            scope: SearchScope,
            searchDeeply: Boolean,
            processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
        ): Boolean = getInstance(ktClass.project).forEachKotlinOverride(ktClass, members, scope, searchDeeply, processor)

        fun PsiMethod.forEachOverridingMethod(
            scope: SearchScope = runReadAction { useScope },
            processor: (PsiMethod) -> Boolean
        ): Boolean = getInstance(project).forEachOverridingMethod(this, scope, processor)

        fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement> =
            getInstance(method.project).findDeepestSuperMethodsNoWrapping(method)

        fun findTypeAliasByShortName(shortName: String, project: Project, scope: GlobalSearchScope): Collection<KtTypeAlias> =
            getInstance(project).findTypeAliasByShortName(shortName, project, scope)

        fun isInProjectSource(element: PsiElement, includeScriptsOutsideSourceRoots: Boolean = false): Boolean =
            getInstance(element.project).isInProjectSource(element, includeScriptsOutsideSourceRoots)

        fun KtDeclaration.isOverridable(): Boolean =
            getInstance(project).isOverridable(this)

        fun KtClass.isInheritable(): Boolean =
            getInstance(project).isInheritable(this)

        fun formatJavaOrLightMethod(method: PsiMethod): String =
            getInstance(method.project).formatJavaOrLightMethod(method)

        fun formatClass(classOrObject: KtClassOrObject): String =
            getInstance(classOrObject.project).formatClass(classOrObject)

        fun KtDeclaration.expectedDeclarationIfAny(): KtDeclaration? =
            getInstance(project).expectedDeclarationIfAny(this)

        fun KtDeclaration.isExpectDeclaration(): Boolean =
            getInstance(project).isExpectDeclaration(this)

        fun KtDeclaration.actualsForExpected(module: Module? = null): Set<KtDeclaration> =
            getInstance(project).actualsForExpected(this, module)

        fun PsiElement.canBeResolvedWithFrontEnd(): Boolean =
            getInstance(project).canBeResolvedWithFrontEnd(this)

        fun createConstructorHandle(ktDeclaration: KtDeclaration): ConstructorCallHandle =
            getInstance(ktDeclaration.project).createConstructorHandle(ktDeclaration)

        fun createConstructorHandle(psiMethod: PsiMethod): ConstructorCallHandle =
            getInstance(psiMethod.project).createConstructorHandle(psiMethod)
    }

    fun actualsForExpected(declaration: KtDeclaration, module: Module? = null): Set<KtDeclaration>

    fun dataClassComponentMethodName(element: KtParameter): String?

    fun hasType(element: KtExpression): Boolean

    fun isSamInterface(psiClass: PsiClass): Boolean

    fun <T : PsiNamedElement> filterDataClassComponentsIfDisabled(elements: List<T>, kotlinOptions: KotlinReferencesSearchOptions): List<T>

    fun isCallableOverrideUsage(reference: PsiReference, declaration: KtNamedDeclaration): Boolean

    fun isUsageInContainingDeclaration(reference: PsiReference, declaration: KtNamedDeclaration): Boolean

    fun isExtensionOfDeclarationClassUsage(reference: PsiReference, declaration: KtNamedDeclaration): Boolean

    fun getReceiverTypeSearcherInfo(psiElement: PsiElement, isDestructionDeclarationSearch: Boolean): ReceiverTypeSearcherInfo?

    fun forceResolveReferences(file: KtFile, elements: List<KtElement>)

    fun scriptDefinitionExists(file: PsiFile): Boolean

    fun getDefaultImports(file: KtFile): List<ImportPath>

    fun forEachKotlinOverride(
        ktClass: KtClass,
        members: List<KtNamedDeclaration>,
        scope: SearchScope,
        searchDeeply: Boolean,
        processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
    ): Boolean

    fun forEachOverridingMethod(
        method: PsiMethod,
        scope: SearchScope,
        processor: (PsiMethod) -> Boolean
    ): Boolean

    fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement>

    fun findSuperMethodsNoWrapping(method: PsiElement): List<PsiElement>

    fun findTypeAliasByShortName(shortName: String, project: Project, scope: GlobalSearchScope): Collection<KtTypeAlias>

    fun isInProjectSource(element: PsiElement, includeScriptsOutsideSourceRoots: Boolean = false): Boolean

    fun isOverridable(declaration: KtDeclaration): Boolean

    fun isInheritable(ktClass: KtClass): Boolean

    fun formatJavaOrLightMethod(method: PsiMethod): String

    fun formatClass(classOrObject: KtClassOrObject): String

    fun expectedDeclarationIfAny(declaration: KtDeclaration): KtDeclaration?

    fun isExpectDeclaration(declaration: KtDeclaration): Boolean

    fun canBeResolvedWithFrontEnd(element: PsiElement): Boolean

    fun createConstructorHandle(ktDeclaration: KtDeclaration): ConstructorCallHandle

    fun createConstructorHandle(psiMethod: PsiMethod): ConstructorCallHandle
}