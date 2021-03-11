/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.goto

import com.intellij.ide.actions.SearchEverywhereClassifier
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.awt.Component
import javax.swing.JList

class KotlinSearchEverywhereClassifier : SearchEverywhereClassifier {
    override fun isClass(o: Any?) = o is KtClassOrObject

    override fun isSymbol(o: Any?) = o is KtNamedDeclaration

    override fun getVirtualFile(o: Any) = (o as? PsiElement)?.containingFile?.virtualFile

    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component? {
        val declaration = (value as? PsiElement)?.unwrapped as? KtNamedDeclaration ?: return null
        return KotlinSearchEverywherePsiRenderer(list).getListCellRendererComponent(list, declaration, index, isSelected, isSelected)
    }
}