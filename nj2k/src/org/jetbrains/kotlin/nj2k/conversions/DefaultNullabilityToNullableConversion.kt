/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKTypeElementImpl

class DefaultNullabilityToNullableConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKTypeElementImpl) return recurse(element)
        if (element.type.nullability != Nullability.Default) return recurse(element)
        return recurse(
            JKTypeElementImpl(element.type.updateNullability(Nullability.Nullable))
        )
    }
}