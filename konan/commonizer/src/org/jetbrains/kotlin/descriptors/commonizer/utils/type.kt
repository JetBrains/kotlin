/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

internal inline val KotlinType.declarationDescriptor: ClassifierDescriptor
    get() = (constructor.declarationDescriptor ?: error("No declaration descriptor found for $constructor"))

internal inline val KotlinType.fqName: FqName
    get() = declarationDescriptor.fqNameSafe

internal val KotlinType.fqNameWithTypeParameters: String
    get() = buildString { buildFqNameWithTypeParameters(this@fqNameWithTypeParameters, HashSet()) }

private fun StringBuilder.buildFqNameWithTypeParameters(type: KotlinType, exploredTypeParameters: MutableSet<KotlinType>) {
    append(type.fqName)

    val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type)
    if (typeParameterDescriptor != null) {
        // N.B this is type parameter type

        if (exploredTypeParameters.add(type.makeNotNullable())) { // print upper bounds once the first time when type parameter type is met
            append(":[")
            typeParameterDescriptor.upperBounds.forEachIndexed { index, upperBound ->
                if (index > 0)
                    append(",")
                buildFqNameWithTypeParameters(upperBound, exploredTypeParameters)
            }
            append("]")
        }
    } else {
        // N.B. this is classifier type

        val arguments = type.arguments
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.forEachIndexed { index, argument ->
                if (index > 0)
                    append(",")

                if (argument.isStarProjection)
                    append("*")
                else {
                    val variance = argument.projectionKind
                    if (variance != Variance.INVARIANT)
                        append(variance).append(" ")
                    buildFqNameWithTypeParameters(argument.type, exploredTypeParameters)
                }
            }
            append(">")
        }
    }

    if (type.isMarkedNullable)
        append("?")
}
