/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.kapt3.stubs.AbstractKDocCommentKeeper
import org.jetbrains.kotlin.light.classes.symbol.isFieldForObjectInstance
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor


class Kapt4KDocCommentKeeper(context: Kapt4ContextForStubGeneration): AbstractKDocCommentKeeper<Kapt4ContextForStubGeneration>(context) {
    fun saveKDocComment(tree: JCTree, psiElement: PsiElement) {
        val ktElement = psiElement.extractOriginalKtDeclaration<KtDeclaration>() ?: return
        val docComment =
            when {
                ktElement is KtProperty ->
                    // Do not place documentation on property accessors of a property with a backing field
                    ktElement.docComment.takeIf { psiElement is PsiField }
                ktElement.docComment == null && ktElement is KtPropertyAccessor -> ktElement.property.docComment
                else -> ktElement.docComment
            } ?: return

        if (psiElement is PsiMethod && psiElement.isConstructor && ktElement is KtClassOrObject) {
            // We don't want the class comment to be duplicated on <init>()
            return
        }

        if (psiElement is PsiField && psiElement.isFieldForObjectInstance) {
            // Do not write KDoc on object instance field
            return
        }

        saveKDocComment(tree, docComment)
    }
}

inline fun <reified T : KtDeclaration> PsiElement.extractOriginalKtDeclaration(): T? {
    // This when is needed to avoid recursion
    val elementToExtract = when (this) {
        is KtLightParameter -> when (kotlinOrigin) {
            null -> method
            else -> return kotlinOrigin as? T
        }
        else -> this
    }

    return when (elementToExtract) {
        is KtLightMember<*> -> {
            val origin = elementToExtract.lightMemberOrigin
            origin?.auxiliaryOriginalElement ?: origin?.originalElement ?: elementToExtract.kotlinOrigin
        }
        is KtLightElement<*, *> -> elementToExtract.kotlinOrigin
        else -> null
    } as? T
}
