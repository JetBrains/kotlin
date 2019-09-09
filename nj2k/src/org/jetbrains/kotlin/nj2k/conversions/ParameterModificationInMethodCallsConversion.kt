/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.hasWritableUsages
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.*


class ParameterModificationInMethodCallsConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethod) return recurse(element)
        val newVariables =
            element.parameters.mapNotNull { parameter ->
                if (parameter.hasWritableUsages(element.block, context)) {
                    val parameterType =
                        if (parameter.isVarArgs) {
                            JKClassType(
                                symbolProvider.provideClassSymbol(parameter.type.type.arrayFqName()),
                                if (parameter.type.type is JKJavaPrimitiveType) emptyList()
                                else listOf(
                                    JKVarianceTypeParameterType(
                                        JKVarianceTypeParameterType.Variance.OUT,
                                        parameter.type.type
                                    )
                                ),
                                Nullability.NotNull
                            )

                        } else parameter.type.type
                    JKLocalVariable(
                        JKTypeElement(parameterType),
                        JKNameIdentifier(parameter.name.value),
                        JKFieldAccessExpression(symbolProvider.provideUniverseSymbol(parameter)),
                        JKMutabilityModifierElement(Mutability.MUTABLE)
                    )
                } else null
            }
        if (newVariables.isNotEmpty()) {
            element.block.statements = listOf(JKDeclarationStatement(newVariables)) + element.block.statements
        }
        return recurse(element)
    }
}