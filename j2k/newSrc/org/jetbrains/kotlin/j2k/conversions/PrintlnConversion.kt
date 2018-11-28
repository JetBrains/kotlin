/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKKtCallExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKMethodSymbol
import org.jetbrains.kotlin.j2k.tree.impl.psi
import org.jetbrains.kotlin.name.ClassId

// TODO: Full special methods conversion
class PrintlnConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        return if (element is JKQualifiedExpression) {
            recurse(processQualified(element))
        } else {
            recurse(element)
        }
    }

    private fun processQualified(element: JKQualifiedExpression): JKExpression {
        val receiver = element.receiver as? JKQualifiedExpression ?: return element
        val classReference = receiver.receiver as? JKClassAccessExpression ?: return element
        if (classReference.identifier.fqName != "java.lang.System") return element
        val fieldReference = receiver.selector as? JKFieldAccessExpression ?: return element
        if (fieldReference.identifier.name != "out") return element
        val selector = element.selector as? JKMethodCallExpression ?: return element
        val functionName = selector.identifier.name
        if (functionName != "println" && functionName != "print") return element

        val contextElement = element.parentOfType<JKClass>() ?: return element
        val targetElements = multiResolveFqName(ClassId.fromString("kotlin/io/$functionName"), contextElement.psi!!)
        if (targetElements.isEmpty()) return element
        selector.invalidate()


        return JKKtCallExpressionImpl(
            context.symbolProvider.provideDirectSymbol(targetElements.first()) as JKMethodSymbol,
            selector.arguments
        )
    }

}