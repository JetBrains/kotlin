/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ReferenceUtils")

package org.jetbrains.kotlin.test.util

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.junit.Assert

@JvmOverloads
fun PsiElement.renderAsGotoImplementation(renderModule: Boolean = false): String {
    val navigationElement = navigationElement

    if (navigationElement is KtObjectDeclaration && navigationElement.isCompanion()) {
        //default presenter return null for companion object
        val containingClass = PsiTreeUtil.getParentOfType(navigationElement, KtClass::class.java)!!
        return "companion object of " + containingClass.renderAsGotoImplementation(renderModule)
    }

    if (navigationElement is KtStringTemplateExpression) {
        return navigationElement.plainContent
    }

    Assert.assertTrue(navigationElement is NavigationItem)
    val presentation = (navigationElement as NavigationItem).presentation

    if (presentation == null) {
        val elementText = text
        return elementText ?: navigationElement.text
    }

    val presentableText = presentation.presentableText
    var locationString = presentation.locationString
    if (locationString == null && parent is PsiAnonymousClass) {
        locationString = "<anonymous>"
    }
    if (renderModule) {
        locationString = "[${navigationElement.module?.name ?: ""}] $locationString"
    }
    return if (locationString == null || navigationElement is PsiPackage)
        presentableText!!
    else
        locationString + "." + presentableText// for PsiPackage, presentableText is FQ name of current package
}