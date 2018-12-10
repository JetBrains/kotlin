/*
     * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
     * that can be found in the license/LICENSE.txt file.
     */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*

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
                element.extraModifiers,
                element.visibility,
                Modality.FINAL,
                mutability
            )
        )
    }
}
