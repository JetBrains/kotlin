/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.types.AbstractStrictEqualityTypeChecker

class TypeCommonizer(private val cache: CirClassifiersCache) : AbstractStandardCommonizer<CirType, CirType>() {
    private lateinit var temp: CirType

    override fun commonizationResult() = temp

    override fun initialize(first: CirType) {
        temp = first
    }

    override fun doCommonizeWith(next: CirType) = areTypesEqual(cache, temp, next)
}

/**
 * See also [AbstractStrictEqualityTypeChecker].
 */
@Suppress("IntroduceWhenSubject")
internal fun areTypesEqual(cache: CirClassifiersCache, a: CirType, b: CirType): Boolean = when {
    a is CirSimpleType -> (b is CirSimpleType) && areSimpleTypesEqual(cache, a, b)
    a is CirFlexibleType -> (b is CirFlexibleType)
            && areSimpleTypesEqual(cache, a.lowerBound, b.lowerBound)
            && areSimpleTypesEqual(cache, a.upperBound, b.upperBound)
    else -> false
}

private fun areSimpleTypesEqual(cache: CirClassifiersCache, a: CirSimpleType, b: CirSimpleType): Boolean {
    val aId = a.classifierId
    val bId = b.classifierId

    if (a !== b) {
        if (a.arguments.size != b.arguments.size || a.isMarkedNullable != b.isMarkedNullable) {
            return false
        }

        if (aId is CirClassifierId.ClassOrTypeAlias) {
            if (bId !is CirClassifierId.ClassOrTypeAlias || aId.classId != bId.classId) return false
        }

        if (aId is CirClassifierId.TypeParameter) {
            if (bId !is CirClassifierId.TypeParameter || aId.index != bId.index) return false
        }
    }

    // N.B. only for descriptors that represent classes or type aliases, but not type parameters!
    fun isClassOrTypeAliasUnderStandardKotlinPackages(): Boolean {
        return (aId as? CirClassifierId.ClassOrTypeAlias)?.classId?.packageFqName?.isUnderStandardKotlinPackages == true
    }

    fun descriptorsCanBeCommonizedThemselves(): Boolean {
        return when (aId) {
            is CirClassifierId.Class -> bId is CirClassifierId.Class && cache.classes[aId.classId].canBeCommonized()
            is CirClassifierId.TypeAlias -> bId is CirClassifierId.TypeAlias && cache.typeAliases[aId.classId].canBeCommonized()
            is CirClassifierId.TypeParameter -> {
                // Real type parameter commonization is performed in TypeParameterCommonizer.
                // Here it is enough to check that FQ names are equal (which is already done above).
                true
            }
        }
    }

    val descriptorsCanBeCommonized =
        /* either class or type alias from Kotlin stdlib */ isClassOrTypeAliasUnderStandardKotlinPackages()
            || /* or descriptors themselves can be commonized */ descriptorsCanBeCommonizedThemselves()

    if (!descriptorsCanBeCommonized)
        return false

    // N.B. both lists of arguments are already known to be of the same size
    for (i in a.arguments.indices) {
        val aArg = a.arguments[i]
        val bArg = b.arguments[i]

        if (aArg.isStarProjection != bArg.isStarProjection)
            return false

        if (!aArg.isStarProjection) {
            if (aArg.projectionKind != bArg.projectionKind)
                return false

            if (!areTypesEqual(cache, aArg.type, bArg.type))
                return false
        }
    }

    return true
}

@Suppress("NOTHING_TO_INLINE")
private inline fun CirNode<*, *>?.canBeCommonized() =
    if (this == null) {
        // No node means that the class or type alias was not subject for commonization at all, probably it lays
        // not in commonized module descriptors but somewhere in their dependencies.
        true
    } else {
        // If entry is present, then contents (common declaration) should not be null.
        commonDeclaration() != null
    }
