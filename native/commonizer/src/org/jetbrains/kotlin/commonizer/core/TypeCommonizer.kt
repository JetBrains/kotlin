/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

class TypeCommonizer(private val classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirType, CirType>() {
    private lateinit var typeCommonizer: Commonizer<*, CirType>

    override fun commonizationResult() = typeCommonizer.result

    override fun initialize(first: CirType) {
        @Suppress("UNCHECKED_CAST")
        typeCommonizer = when (first) {
            is CirClassOrTypeAliasType -> ClassOrTypeAliasTypeCommonizer(classifiers)
            is CirTypeParameterType -> TypeParameterTypeCommonizer()
            is CirFlexibleType -> FlexibleTypeCommonizer(classifiers)
        } as Commonizer<*, CirType>
    }

    override fun doCommonizeWith(next: CirType): Boolean {
        return when (next) {
            is CirClassOrTypeAliasType -> (typeCommonizer as? ClassOrTypeAliasTypeCommonizer)?.commonizeWith(next) == true
            is CirTypeParameterType -> (typeCommonizer as? TypeParameterTypeCommonizer)?.commonizeWith(next) == true
            is CirFlexibleType -> (typeCommonizer as? FlexibleTypeCommonizer)?.commonizeWith(next) == true
        }
    }
}

private class TypeParameterTypeCommonizer : AbstractStandardCommonizer<CirTypeParameterType, CirTypeParameterType>() {
    private lateinit var temp: CirTypeParameterType

    override fun commonizationResult() = temp

    override fun initialize(first: CirTypeParameterType) {
        temp = first
    }

    override fun doCommonizeWith(next: CirTypeParameterType): Boolean {
        // Real type parameter commonization is performed in TypeParameterCommonizer.
        // Here it is enough to check that type parameter indices and nullability are equal.
        return temp == next
    }
}

private class FlexibleTypeCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirFlexibleType, CirFlexibleType>() {
    private val lowerBound = TypeCommonizer(classifiers)
    private val upperBound = TypeCommonizer(classifiers)

    override fun commonizationResult() = CirFlexibleType(
        lowerBound = lowerBound.result as CirSimpleType,
        upperBound = upperBound.result as CirSimpleType
    )

    override fun initialize(first: CirFlexibleType) = Unit

    override fun doCommonizeWith(next: CirFlexibleType) =
        lowerBound.commonizeWith(next.lowerBound) && upperBound.commonizeWith(next.upperBound)
}
