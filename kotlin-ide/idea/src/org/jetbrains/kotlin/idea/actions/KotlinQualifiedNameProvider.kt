/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.ide.actions.JavaQualifiedNameProvider
import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class KotlinQualifiedNameProvider : QualifiedNameProvider {
    override fun adjustElementToCopy(element: PsiElement?) = null

    override fun getQualifiedName(element: PsiElement?) = when (element) {
        is KtClassOrObject -> element.fqName?.asString()
        is KtNamedFunction -> getJavaQualifiedName(LightClassUtil.getLightClassMethod(element))

        is KtProperty -> {
            val lightClassPropertyMethods = LightClassUtil.getLightClassPropertyMethods(element)
            val lightElement: PsiElement? = lightClassPropertyMethods.getter ?: lightClassPropertyMethods.backingField
            getJavaQualifiedName(lightElement)
        }
        else -> null
    }

    private fun getJavaQualifiedName(element: PsiElement?) = element?.let { JavaQualifiedNameProvider().getQualifiedName(element) }

    override fun qualifiedNameToElement(fqn: String?, project: Project?) = null

    override fun insertQualifiedName(fqn: String?, element: PsiElement?, editor: Editor?, project: Project?) {
    }
}
