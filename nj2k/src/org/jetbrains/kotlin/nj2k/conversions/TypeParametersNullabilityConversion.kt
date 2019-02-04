/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.j2k.ast.Nullability
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.*


class TypeParametersNullabilityConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKTypeParameterList) return recurse(element)
        val typeParametersNullabilityMap =
            element.typeParameters.map { typeParameter ->
                typeParameter.name.value to typeParameter.hasNullableUpperBounds()
            }.toMap()
        return JKTypeParameterListImpl(
            element.typeParameters.map { typeParameter ->
                val name = typeParameter.name.value
                JKTypeParameterImpl(
                    JKNameIdentifierImpl(name),
                    typeParameter.upperBounds.map { upperBoundTypeElement ->
                        JKTypeElementImpl(
                            upperBoundTypeElement.type.makeTypeParametersNotNull(typeParametersNullabilityMap)
                        )
                    }
                )
            }
        )
    }

    private fun JKType.makeTypeParametersNotNull(nullabilityMap: Map<String, Boolean>): JKType =
        when (this) {
            is JKClassType ->
                JKClassTypeImpl(
                    classReference,
                    parameters.map { it.makeTypeParametersNotNull(nullabilityMap) },
                    nullability
                )
            is JKTypeParameterType ->
                nullabilityMap[name]
                    ?.takeIf { nullability == Nullability.Default }
                    ?.let { isNullable ->
                        JKTypeParameterTypeImpl(
                            name,
                            if (isNullable) Nullability.NotNull else Nullability.Nullable
                        )
                    } ?: this

            is JKVarianceTypeParameterType ->
                JKVarianceTypeParameterTypeImpl(
                    variance,
                    boundType.makeTypeParametersNotNull(nullabilityMap)
                )
            else -> this
        }

    private fun JKTypeParameter.hasNullableUpperBounds(): Boolean =
        upperBounds.any { it.type.isNullable() }
}