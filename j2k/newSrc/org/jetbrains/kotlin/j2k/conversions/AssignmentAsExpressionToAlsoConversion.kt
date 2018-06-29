/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.JKExpressionStatement
import org.jetbrains.kotlin.j2k.tree.JKJavaAssignmentExpression
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.*
import org.jetbrains.kotlin.name.ClassId

class AssignmentAsExpressionToAlsoConversion(val context: ConversionContext) : RecursiveApplicableConversionBase() {

    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaAssignmentExpression || element.parent !is JKExpressionStatement) return recurse(element)


        val alsoElement = resolveFqName(ClassId.fromString("kotlin/also"), element, context) ?: return recurse(element)
        val alsoSymbol = context.symbolProvider.provideDirectSymbol(alsoElement) as? JKMethodSymbol ?: return recurse(element)
        element.invalidate()

        return JKQualifiedExpressionImpl(
            element.expression,
            JKKtQualifierImpl.DOT,
            JKKtCallExpressionImpl(alsoSymbol, JKExpressionListImpl(
                // TODO: Lambda expression here
            ))
        )
    }
}