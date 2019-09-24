/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.asQualifierWithThisAsSelector
import org.jetbrains.kotlin.nj2k.symbols.JKClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.isStaticMember
import org.jetbrains.kotlin.nj2k.tree.*

class StaticMemberAccessConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        val symbol = when (element) {
            is JKFieldAccessExpression -> element.identifier
            is JKCallExpression -> element.identifier
            else -> null
        } ?: return recurse(element)
        if (element.asQualifierWithThisAsSelector() != null) return recurse(element)
        if (symbol.isStaticMember) {
            val containingClassSymbol = symbol.declaredIn as? JKClassSymbol ?: return recurse(element)
            return recurse(
                JKQualifiedExpression(
                    JKClassAccessExpression(containingClassSymbol),
                    element.copyTreeAndDetach() as JKExpression
                )
            )
        }
        return recurse(element)
    }
}

