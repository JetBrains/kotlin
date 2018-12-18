/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtThrowExpressionImpl


class EnumClassConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClass || element.classKind != JKClass.ClassKind.ENUM) return recurse(element)
        element.modality = Modality.FINAL
        return recurse(element)
    }
}