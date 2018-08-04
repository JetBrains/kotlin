/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKNullLiteral
import org.jetbrains.kotlin.j2k.tree.impl.JKUnresolvedClassType

class ImplicitInitializerConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaField) return recurse(element)

        if (element.initializer !is JKStubExpression) return recurse(element)

        if (element.type.type is JKClassType || element.type.type is JKUnresolvedClassType) element.initializer = JKNullLiteral()

        return element
    }

}