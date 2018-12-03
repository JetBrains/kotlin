/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKFieldAccessExpressionImpl


class CollectionOperationsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        val receiverType = element.receiver.type(context) ?: return recurse(element)
        if (!receiverType.isCollectionType(context.symbolProvider)) return recurse(element)

        convertSizeCall(element.selector)?.also {
            element.selector = it
        }

        return recurse(element)
    }

    private fun convertSizeCall(selector: JKExpression): JKFieldAccessExpressionImpl? {
        if (selector !is JKMethodCallExpression) return null
        return if (selector.identifier.name =="size")  {
            JKFieldAccessExpressionImpl(context.symbolProvider.provideByFqName("kotlin.collections.Collection.size"))
        } else null
    }
}