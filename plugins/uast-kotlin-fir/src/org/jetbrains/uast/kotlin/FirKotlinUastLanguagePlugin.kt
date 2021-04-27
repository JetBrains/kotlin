/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.DEFAULT_TYPES_LIST
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.kotlin.FirKotlinConverter.convertDeclarationOrElement

interface FirKotlinUastResolveProviderService {
    fun isJvmElement(psiElement: PsiElement): Boolean
}

class FirKotlinUastLanguagePlugin : UastLanguagePlugin {
    override val priority: Int = 10

    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun isFileSupported(fileName: String): Boolean {
        return fileName.endsWith(".kt", false) || fileName.endsWith(".kts", false)
    }

    private val PsiElement.isJvmElement: Boolean
        get() = service.isJvmElement(this)

    override fun convertElement(element: PsiElement, parent: UElement?, requiredType: Class<out UElement>?): UElement? {
        if (!element.isJvmElement) return null
        return convertDeclarationOrElement(element, parent, elementTypes(requiredType))
    }

    override fun convertElementWithParent(element: PsiElement, requiredType: Class<out UElement>?): UElement? {
        if (!element.isJvmElement) return null
        return convertDeclarationOrElement(element, null, elementTypes(requiredType))
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : UElement> convertElementWithParent(element: PsiElement, requiredTypes: Array<out Class<out T>>): T? {
        if (!element.isJvmElement) return null
        val nonEmptyRequiredTypes = requiredTypes.nonEmptyOr(DEFAULT_TYPES_LIST)
        return convertDeclarationOrElement(element, null, nonEmptyRequiredTypes) as? T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : UElement> convertToAlternatives(element: PsiElement, requiredTypes: Array<out Class<out T>>): Sequence<T> {
        if (!element.isJvmElement) return emptySequence()
        return when {
            element is KtFile ->
                FirKotlinConverter.convertKtFile(element, null, requiredTypes) as Sequence<T>
            element is KtClassOrObject ->
                FirKotlinConverter.convertClassOrObject(element, null, requiredTypes) as Sequence<T>
            element is KtProperty && !element.isLocal ->
                FirKotlinConverter.convertPsiElement(element, null, requiredTypes) as Sequence<T>
            element is KtParameter ->
                FirKotlinConverter.convertParameter(element, null, requiredTypes) as Sequence<T>
            else ->
                sequenceOf(convertElementWithParent(element, requiredTypes.nonEmptyOr(DEFAULT_TYPES_LIST)) as? T).filterNotNull()
        }
    }

    override fun getConstructorCallExpression(
        element: PsiElement,
        fqName: String
    ): UastLanguagePlugin.ResolvedConstructor? {
        TODO("Not yet implemented")
    }

    override fun getMethodCallExpression(
        element: PsiElement,
        containingClassFqName: String?,
        methodName: String
    ): UastLanguagePlugin.ResolvedMethod? {
        TODO("Not yet implemented")
    }

    override fun isExpressionValueUsed(element: UExpression): Boolean {
        TODO("Not yet implemented")
    }
}
