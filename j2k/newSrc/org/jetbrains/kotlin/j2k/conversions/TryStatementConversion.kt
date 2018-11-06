/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.copyTreeAndDetach
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*


class TryStatementConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaTryStatement) return recurse(element)
        val tryExpression = JKKtTryExpressionImpl(
            element::tryBlock.detached(),
            element::finallyBlock.detached(),
            element.catchSections.flatMap(::convertCatchSection)
        )
        return recurse(JKExpressionStatementImpl(tryExpression))
    }

    private fun convertCatchSection(javaCatchSection: JKJavaTryCatchSection): List<JKKtTryCatchSection> {
        javaCatchSection.block.detach(javaCatchSection)
        return javaCatchSection.parameter.type.type.let {
            (it as? JKJavaDisjunctionType)?.disjunctions ?: listOf(it)
        }.map {
            val parameter = JKParameterImpl(
                JKTypeElementImpl(it),
                javaCatchSection.parameter.name.copyTreeAndDetach(),
                JKModifierListImpl()
            )
            JKKtTryCatchSectionImpl(
                parameter,
                javaCatchSection.block.copyTreeAndDetach()
            )
        }
    }
}