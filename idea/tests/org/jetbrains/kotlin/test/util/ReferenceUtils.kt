/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("ReferenceUtils")

package org.jetbrains.kotlin.test.util

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.junit.Assert

fun PsiElement.renderAsGotoImplementation(): String {
    val navigationElement = navigationElement

    if (navigationElement is KtObjectDeclaration && navigationElement.isCompanion()) {
        //default presenter return null for companion object
        val containingClass = PsiTreeUtil.getParentOfType(navigationElement, KtClass::class.java)!!
        return "companion object of " + containingClass.renderAsGotoImplementation()
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
    return if (locationString == null || navigationElement is PsiPackage)
        presentableText!!
    else
        locationString + "." + presentableText// for PsiPackage, presentableText is FQ name of current package
}