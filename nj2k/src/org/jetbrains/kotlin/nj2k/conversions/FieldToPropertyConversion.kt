/*
     * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiField
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.JKFieldDataFromJava
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.psi
import org.jetbrains.kotlin.nj2k.tree.*


class FieldToPropertyConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKField) return recurse(element)
        element.mutability = if (element.modality == Modality.FINAL) Mutability.IMMUTABLE else Mutability.MUTABLE

        element.psi<PsiField>()?.let { psi ->
            context.externalCodeProcessor.addMember(JKFieldDataFromJava(psi))
        }

        element.modality = Modality.FINAL
        return recurse(element)
    }
}
