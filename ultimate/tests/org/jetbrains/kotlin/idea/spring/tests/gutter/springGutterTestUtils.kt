/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.tests.gutter

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.spring.SpringPresentationProvider
import com.intellij.spring.model.pom.SpringBeanPomTargetUtils
import com.intellij.spring.model.xml.DomSpringBean
import com.intellij.spring.model.xml.beans.SpringPropertyDefinition
import com.intellij.testFramework.UsefulTestCase.assertSameElements
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.xml.DomUtil
import org.jetbrains.kotlin.idea.completion.test.assertInstanceOf

private fun nameBean(element: PsiElement): String {
    return when (element) {
        is XmlTag -> DomUtil.getDomElement(element).assertInstanceOf<DomSpringBean>().beanName!!
        is PomTargetPsiElement -> SpringBeanPomTargetUtils.getSpringBean(element)?.let { SpringPresentationProvider.getSpringBeanName(it) }
                                  ?: namePsi(element)
        else -> throw IllegalArgumentException("Cannot convert " + element.javaClass + " (" + element.text + ")")
    }
}

private fun nameProperty(element: PsiElement) = DomUtil.getDomElement(element).assertInstanceOf<SpringPropertyDefinition>().propertyName!!

private fun namePsi(element: PsiElement) = SymbolPresentationUtil.getSymbolPresentableText(element)

fun checkBeanGutterTargets(gutterMark: GutterMark, expectedBeanNames: Collection<String>) {
    checkGutterTargets(gutterMark, expectedBeanNames, ::nameBean)
}

fun checkBeanPropertyTargets(gutterMark: GutterMark, expectedPropertyNames: Collection<String>) {
    checkGutterTargets(gutterMark, expectedPropertyNames, ::nameProperty)
}

fun checkPsiElementGutterTargets(gutterMark: GutterMark, expectedSymbolNames: Collection<String>) {
    checkGutterTargets(gutterMark, expectedSymbolNames, ::namePsi)
}

fun checkGutterTargets(
        gutterMark: GutterMark,
        expectedValues: Collection<String>,
        targetNamer: (PsiElement) -> String
) {
    val targetElements = when (gutterMark) {
        is LineMarkerInfo.LineMarkerGutterIconRenderer<*> ->
            gutterMark.lineMarkerInfo.navigationHandler.assertInstanceOf<NavigationGutterIconRenderer>().targetElements
        is NavigationGutterIconRenderer ->
            gutterMark.targetElements
        else -> throw IllegalArgumentException("${gutterMark.javaClass.name} not supported")
    }

    assertSameElements(ContainerUtil.map(targetElements, targetNamer), expectedValues)
}