/*
     * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*

class FieldToPropertyConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaField) return recurse(element)
        element.invalidate()
        val mutability =
            if (element.modality == Modality.FINAL) Mutability.IMMUTABLE
            else Mutability.MUTABLE
        return recurse(
            JKKtPropertyImpl(
                element.type,
                element.name,
                element.initializer,
                JKKtEmptyGetterOrSetterImpl(),
                JKKtEmptyGetterOrSetterImpl(),
                element.annotationList,
                element.otherModifierElements,
                element.visibilityElement,
                JKModalityModifierElementImpl(Modality.FINAL),
                JKMutabilityModifierElementImpl(mutability).withNonCodeElementsFrom(element.modalityElement)
            ).also {
                it.psi = element.psi
                it.takeNonCodeElementsFrom(element)
            }
        )
    }
}
