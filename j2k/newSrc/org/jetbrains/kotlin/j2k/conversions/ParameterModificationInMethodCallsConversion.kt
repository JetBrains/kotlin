/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.asAssignmentFromTarget
import org.jetbrains.kotlin.j2k.findUsages
import org.jetbrains.kotlin.j2k.hasWritableUsages
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.*


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
                        Mutability.MUTABLE
                    )
                } else null
            }
        element.block.statements = listOf(JKDeclarationStatementImpl(newVariables)) + element.block.statements
        return recurse(element)
    }
}