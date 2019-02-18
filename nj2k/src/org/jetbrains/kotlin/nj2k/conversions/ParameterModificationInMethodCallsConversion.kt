/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.hasWritableUsages
import org.jetbrains.kotlin.nj2k.tree.JKMethod
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement
import org.jetbrains.kotlin.nj2k.tree.Mutability
import org.jetbrains.kotlin.nj2k.tree.impl.*


class ParameterModificationInMethodCallsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethod) return recurse(element)
        val newVariables =
            element.parameters.mapNotNull { parameter ->
                if (parameter.hasWritableUsages(element.block, context)) {
                    JKLocalVariableImpl(
                        JKTypeElementImpl(parameter.type.type),
                        JKNameIdentifierImpl(parameter.name.value),
                        JKFieldAccessExpressionImpl(context.symbolProvider.provideUniverseSymbol(parameter)),
                        JKMutabilityModifierElementImpl(Mutability.MUTABLE)
                    )
                } else null
            }
        if (newVariables.isNotEmpty()) {
            element.block.statements = listOf(JKDeclarationStatementImpl(newVariables)) + element.block.statements
        }
        return recurse(element)
    }
}