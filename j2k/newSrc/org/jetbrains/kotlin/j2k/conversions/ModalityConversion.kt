/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.psi

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
        if (klass.modality != Modality.OPEN) return
        if (context.converter.settings.openByDefault) return
        if (!context.converter.converterServices.oldServices.referenceSearcher.hasInheritors(klass.psi as PsiClass)) {
            klass.modality = Modality.FINAL
        }
    }

    private fun processMethod(method: JKJavaMethod) {
        val psi = method.psi<PsiMethod>()!!
        if (method.modality == Modality.OPEN
            && !context.converter.converterServices.oldServices.referenceSearcher.hasOverrides(psi)
        ) {
            method.modality = Modality.FINAL
        }
        if ((method.psi!! as PsiMethod).findSuperMethods().isNotEmpty()) {
            method.modality = Modality.OVERRIDE
        }
    }

}