/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.psi

class ModalityConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        when (element) {
            is JKClass -> processClass(element)
            is JKJavaMethod -> processMethod(element)
            is JKField -> processField(element)
        }
        return recurse(element)
    }

    private fun processClass(klass: JKClass) {
        klass.modality = when {
            klass.classKind == JKClass.ClassKind.ENUM -> Modality.FINAL
            klass.classKind == JKClass.ClassKind.INTERFACE -> Modality.OPEN
            klass.modality == Modality.OPEN
                    && context.converter.settings.openByDefault -> Modality.OPEN
            klass.modality == Modality.OPEN
                    && !context.converter.converterServices.oldServices.referenceSearcher.hasInheritors(klass.psi as PsiClass) ->
                Modality.FINAL

            else -> klass.modality
        }
    }

    private fun processMethod(method: JKJavaMethod) {
        val psi = method.psi<PsiMethod>() ?: return
        val containingClass = method.parentOfType<JKClass>() ?: return
        method.modality = when {
            method.modality != Modality.ABSTRACT
                    && psi.findSuperMethods().isNotEmpty() -> Modality.OVERRIDE
            method.modality == Modality.OPEN
                    && context.converter.settings.openByDefault
                    && containingClass.modality == Modality.OPEN
                    && method.visibility != Visibility.PRIVATE -> Modality.OPEN

            method.modality == Modality.OPEN
                    && containingClass.classKind != JKClass.ClassKind.INTERFACE
                    && !context.converter.converterServices.oldServices.referenceSearcher.hasOverrides(psi) -> Modality.FINAL
            else -> method.modality
        }
    }

    private fun processField(field: JKField) {
        val containingClass = field.parentOfType<JKClass>() ?: return
        if (containingClass.classKind == JKClass.ClassKind.INTERFACE) {
            field.modality = Modality.FINAL
        }
    }
}