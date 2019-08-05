/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.isClassType

interface TypeCommonizer : Commonizer<KotlinType, UnwrappedType> {
    companion object {
        fun default(): TypeCommonizer = DefaultTypeCommonizer()
    }
}

private class DefaultTypeCommonizer : TypeCommonizer {
    private enum class State {
        EMPTY,
        ERROR,
        IN_PROGRESS
    }

    private var state = State.EMPTY
    private var temp: UnwrappedType? = null

    override val result: UnwrappedType
        get() = when (state) {
            State.EMPTY, State.ERROR -> error("Can't commonize type")
            State.IN_PROGRESS -> temp!!
        }

    override fun commonizeWith(next: KotlinType): Boolean {
        state = when (state) {
            State.ERROR -> State.ERROR
            State.EMPTY -> {
                temp = next.unwrap()
                State.IN_PROGRESS
            }
            // TODO: maybe cache type comparison results?
            State.IN_PROGRESS -> {
                if (!areTypesEqual(temp!!, next.unwrap())) State.ERROR else State.IN_PROGRESS
            }
        }

        return state != State.ERROR
    }
}

/**
 * See also [AbstractStrictEqualityTypeChecker].
 */
internal fun areTypesEqual(a: UnwrappedType, b: UnwrappedType): Boolean = when {
    a === b -> true
    a is SimpleType -> (b is SimpleType) && areSimpleTypesEqual(a, b)
    a is FlexibleType -> (b is FlexibleType)
            && areSimpleTypesEqual(a.lowerBound, b.lowerBound) && areSimpleTypesEqual(a.upperBound, b.upperBound)
    else -> false
}

private fun areSimpleTypesEqual(a: SimpleType, b: SimpleType): Boolean = areAbbreviatedTypesEqual(
    a = a.getAbbreviation() ?: a,
    aExpanded = a,
    b = b.getAbbreviation() ?: b,
    bExpandedType = b
)

private fun areAbbreviatedTypesEqual(a: SimpleType, aExpanded: SimpleType, b: SimpleType, bExpandedType: SimpleType): Boolean {
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

    if (aFqName.isUnderStandardKotlinPackages || bFqName.isUnderStandardKotlinPackages) {
        // make sure that FQ names of abbreviated types (e.g. representing type aliases) are equal
        return aFqName == bFqName
                // if classes are from the standard Kotlin packages, compare them only by type constructors
                // effectively, this includes 1) comparison of FQ names and 2) number of type constructor parameters
                // see org.jetbrains.kotlin.types.AbstractClassTypeConstructor.equals() for details
                && aExpanded.constructor == bExpandedType.constructor
    }

    val descriptorsCanBeCommonized = when (aDescriptor) {
        is TypeAliasDescriptor -> (bDescriptor is TypeAliasDescriptor) && canBeCommonized(aDescriptor, bDescriptor)
        is ClassDescriptor -> (bDescriptor is ClassDescriptor) && canBeCommonized(aDescriptor, bDescriptor)
        else -> false
    }

    if (!descriptorsCanBeCommonized)
        return false

    if (a.arguments === b.arguments)
        return true

    for (i in 0 until a.arguments.size) {
        val aArg = a.arguments[i]
        val bArg = b.arguments[i]

        if (aArg.isStarProjection != bArg.isStarProjection)
            return false

        if (!aArg.isStarProjection) {
            if (aArg.projectionKind != bArg.projectionKind)
                return false

            if (!areTypesEqual(aArg.type.unwrap(), bArg.type.unwrap()))
                return false
        }
    }

    return true
}

@Suppress("NOTHING_TO_INLINE")
private inline fun SimpleType.nonNullDescriptorExpectedErrorMessage() =
    "${TypeCommonizer::class} couldn't obtain non-null descriptor from: $this, ${this::class}"

private val standardKotlinPackages = setOf(
    KotlinBuiltIns.BUILT_INS_PACKAGE_NAME,
    Name.identifier("kotlinx")
)

private val FqName.isUnderStandardKotlinPackages: Boolean
    get() = pathSegments().firstOrNull() in standardKotlinPackages

// TODO: extract this method to class commonizer
private fun canBeCommonized(a: ClassDescriptor, b: ClassDescriptor) = when {
    a.kind != b.kind -> false
    !areFqNamesEqual(a, b) -> false
    // TODO: compare class descriptors (visibility, modifiers, etc)
    else -> true
}

// TODO: extract this method to type alias commonizer
private fun canBeCommonized(a: TypeAliasDescriptor, b: TypeAliasDescriptor): Boolean {
    if (!areFqNamesEqual(a, b))
        return false

    val aUnderlyingType = a.underlyingType
    val bUnderlyingType = b.underlyingType

    if (aUnderlyingType.arguments.isNotEmpty() || bUnderlyingType.arguments.isNotEmpty())
        return false // type aliases with functional types at the right-hand side can't be commonized

    if (!aUnderlyingType.isClassType || !bUnderlyingType.isClassType)
        return false // right-hand side could have only classes

    return areTypesEqual(aUnderlyingType, bUnderlyingType)
}

private fun <T : ClassifierDescriptor> areFqNamesEqual(d1: T, d2: T): Boolean {
    val p1 = d1.parentsWithSelf.iterator()
    val p2 = d2.parentsWithSelf.iterator()

    while (p1.hasNext() && p2.hasNext()) {
        val n1 = p1.next()
        val n2 = p2.next()

        when (n1) {
            is ModuleDescriptor -> return n2 is ModuleDescriptor
            is PackageFragmentDescriptor -> return (n2 is PackageFragmentDescriptor) && n1.fqName == n2.fqName
            else -> if (n1.name != n2.name) return false
        }
    }

    return false
}
