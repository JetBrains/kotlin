/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Node
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*

interface TypeCommonizer : Commonizer<KotlinType, UnwrappedType> {
    companion object {
        fun default(cache: ClassifiersCache): TypeCommonizer = DefaultTypeCommonizer(cache)
    }
}

private class DefaultTypeCommonizer(private val cache: ClassifiersCache) :
    TypeCommonizer,
    AbstractStandardCommonizer<KotlinType, UnwrappedType>() {

    private lateinit var temp: UnwrappedType

    override fun commonizationResult() = temp

    override fun initialize(first: KotlinType) {
        temp = first.unwrap()
    }

    override fun doCommonizeWith(next: KotlinType) = areTypesEqual(cache, temp, next.unwrap())
}

/**
 * See also [AbstractStrictEqualityTypeChecker].
 */
internal fun areTypesEqual(cache: ClassifiersCache, a: UnwrappedType, b: UnwrappedType): Boolean = when {
    a === b -> true
    a is SimpleType -> (b is SimpleType) && areSimpleTypesEqual(cache, a, b)
    a is FlexibleType -> (b is FlexibleType)
            && areSimpleTypesEqual(cache, a.lowerBound, b.lowerBound) && areSimpleTypesEqual(cache, a.upperBound, b.upperBound)
    else -> false
}

private fun areSimpleTypesEqual(cache: ClassifiersCache, a: SimpleType, b: SimpleType): Boolean = areAbbreviatedTypesEqual(
    cache,
    a = a.getAbbreviation() ?: a,
    aExpanded = a,
    b = b.getAbbreviation() ?: b,
    bExpandedType = b
)

private fun areAbbreviatedTypesEqual(
    cache: ClassifiersCache,
    a: SimpleType,
    aExpanded: SimpleType,
    b: SimpleType,
    bExpandedType: SimpleType
): Boolean {
    if (a.arguments.size != b.arguments.size
        || a.isMarkedNullable != b.isMarkedNullable
        || a.isDefinitelyNotNullType != b.isDefinitelyNotNullType
    ) {
        return false
    }

    val aDescriptor = requireNotNull(a.constructor.declarationDescriptor, a::nonNullDescriptorExpectedErrorMessage)
    val bDescriptor = requireNotNull(b.constructor.declarationDescriptor, b::nonNullDescriptorExpectedErrorMessage)

    val aFqName = aDescriptor.fqNameSafe
    val bFqName = bDescriptor.fqNameSafe

    if (aFqName != bFqName)
        return false

    val isClassOrTypeAliasUnderStandardKotlinPackages =
        // N.B. only for descriptors that represent classes or type aliases, but not type parameters!
        aDescriptor is ClassifierDescriptorWithTypeParameters
                && bDescriptor is ClassifierDescriptorWithTypeParameters
                && aFqName.isUnderStandardKotlinPackages
                // If classes are from the standard Kotlin packages, compare them only by type constructors.
                // Effectively, this includes 1) comparison of FQ names and 2) number of type constructor parameters.
                // See org.jetbrains.kotlin.types.AbstractClassTypeConstructor.equals() for details.
                && aExpanded.constructor == bExpandedType.constructor

    val descriptorsCanBeCommonized =
        /* either class or type alias from Kotlin stdlib */ isClassOrTypeAliasUnderStandardKotlinPackages
            || /* or descriptors themselves can be commonized */ when (aDescriptor) {
        is TypeParameterDescriptor -> {
            // Real type parameter commonization is performed in TypeParameterCommonizer.
            // Here it is enough to check that FQ names are equal (already done above).
            bDescriptor is TypeParameterDescriptor
        }
        is ClassDescriptor -> {
            (bDescriptor is ClassDescriptor) && cache.classes[aFqName].canBeCommonized()
        }
        is TypeAliasDescriptor -> {
            (bDescriptor is TypeAliasDescriptor) && cache.typeAliases[aFqName].canBeCommonized()
        }
        else -> false
    }

    if (!descriptorsCanBeCommonized)
        return false

    // N.B. both lists of arguments are already known to be of the same size
    for (i in 0 until a.arguments.size) {
        val aArg = a.arguments[i]
        val bArg = b.arguments[i]

        if (aArg.isStarProjection != bArg.isStarProjection)
            return false

        if (!aArg.isStarProjection) {
            if (aArg.projectionKind != bArg.projectionKind)
                return false

            if (!areTypesEqual(cache, aArg.type.unwrap(), bArg.type.unwrap()))
                return false
        }
    }

    return true
}

@Suppress("NOTHING_TO_INLINE")
private inline fun SimpleType.nonNullDescriptorExpectedErrorMessage() =
    "${TypeCommonizer::class} couldn't obtain non-null descriptor from: $this, ${this::class}"

@Suppress("NOTHING_TO_INLINE")
private inline fun Node<*, *>?.canBeCommonized() =
    if (this == null) {
        // No node means that the class or type alias was not subject for commonization at all, probably it lays
        // not in commonized module descriptors but somewhere in their dependencies.
        true
    } else {
        // If entry is present, then contents (common declaration) should not be null.
        common() != null
    }
