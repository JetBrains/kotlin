/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.modality

class ModalityConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {


    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKClass -> processClass(element)
            is JKJavaMethod -> processMethod(element)
        }
        return recurse(element)
    }

    private fun processClass(klass: JKClass) {
        if (klass.classKind != JKClass.ClassKind.CLASS) return
        if (klass.modifierList.modality != JKModalityModifier.Modality.OPEN) return
        if (context.converter.settings.openByDefault) return
        if (!context.converter.converterServices.oldServices.referenceSearcher.hasInheritors(context.backAnnotator(klass) as PsiClass)) {
            klass.modifierList.modality = JKModalityModifier.Modality.FINAL
        }
    }

    private fun processMethod(method: JKJavaMethod) {
        if ((context.backAnnotator(method)!! as PsiMethod).findSuperMethods().isNotEmpty()) {
            method.modifierList.modality = JKModalityModifier.Modality.OVERRIDE
        }
    }

}