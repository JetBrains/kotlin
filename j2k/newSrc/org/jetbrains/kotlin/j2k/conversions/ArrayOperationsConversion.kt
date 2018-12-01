/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKExpressionStatementImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKFieldAccessExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKFieldSymbol
import org.jetbrains.kotlin.j2k.tree.impl.JKKtThrowExpressionImpl


class ArrayOperationsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        val selector = element.selector as? JKFieldAccessExpression ?: return recurse(element)
        if (element.receiver.type(context) !is JKJavaArrayType) return recurse(element)
        if (selector.identifier.name == "length") {
            val sizeCall =
                    JKFieldAccessExpressionImpl(
                        context.symbolProvider.provideByFqName("kotlin/Array.size")
                    )
            element.selector = sizeCall
        }
        return recurse(element)
    }
}