/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

class TypeCommonizer(private val classifiers: CirKnownClassifiers) : AssociativeCommonizer<CirType> {
    override fun commonize(first: CirType, second: CirType): CirType? {
        if (first is CirClassOrTypeAliasType && second is CirClassOrTypeAliasType) {
            return ClassOrTypeAliasTypeCommonizer(classifiers).commonize(first, second)
        }

        if (first is CirTypeParameterType && second is CirTypeParameterType) {
            return TypeParameterTypeCommonizer.commonize(first, second)
        }

        if (first is CirFlexibleType && second is CirFlexibleType) {
            return FlexibleTypeAssociativeCommonizer(classifiers).commonize(first, second)
        }

        return null
    }
}

private object TypeParameterTypeCommonizer : AssociativeCommonizer<CirTypeParameterType> {
    override fun commonize(first: CirTypeParameterType, second: CirTypeParameterType): CirTypeParameterType? {
        // Real type parameter commonization is performed in TypeParameterCommonizer.
        // Here it is enough to check that type parameter indices and nullability are equal.
        if (first == second) return first
        return null
    }
}

private class FlexibleTypeAssociativeCommonizer(private val classifiers: CirKnownClassifiers) : AssociativeCommonizer<CirFlexibleType> {
    override fun commonize(first: CirFlexibleType, second: CirFlexibleType): CirFlexibleType? {

        val lowerBound = TypeCommonizer(classifiers).commonize(first.lowerBound, second.lowerBound) ?: return null
        val upperBound = TypeCommonizer(classifiers).commonize(first.upperBound, second.upperBound) ?: return null

        return CirFlexibleType(
            lowerBound = lowerBound as CirSimpleType,
            upperBound = upperBound as CirSimpleType
        )
    }
}
