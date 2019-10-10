/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils.getTypeParameterDescriptorOrNull
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

internal fun <T> Sequence<T>.toList(expectedCapacity: Int): List<T> {
    val result = ArrayList<T>(expectedCapacity)
    toCollection(result)
    return result
}

internal inline fun <reified T> Iterable<T?>.firstNonNull() = firstIsInstance<T>()

internal inline val KotlinType.declarationDescriptor: ClassifierDescriptor
    get() = (constructor.declarationDescriptor ?: error("No declaration descriptor found for $constructor"))

internal inline val KotlinType.fqName: FqName
    get() = declarationDescriptor.fqNameSafe

internal val KotlinType.fqNameWithTypeParameters: String
    get() = buildString { buildFqNameWithTypeParameters(this@fqNameWithTypeParameters, HashSet()) }

private fun StringBuilder.buildFqNameWithTypeParameters(type: KotlinType, exploredTypeParameters: MutableSet<KotlinType>) {
    append(type.fqName)

    val typeParameterDescriptor = getTypeParameterDescriptorOrNull(type)
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

internal fun Any?.isNull(): Boolean = this == null

private val KOTLINX_PACKAGE_NAME = Name.identifier("kotlinx")

internal val FqName.isUnderStandardKotlinPackages: Boolean
    get() = startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME) || startsWith(KOTLINX_PACKAGE_NAME)
