/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirTypeAliasType
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

internal class ClassOrTypeAliasTypeCommonizer(
    private val classifiers: CirKnownClassifiers
) : AssociativeCommonizer<CirClassOrTypeAliasType> {

    override fun commonize(first: CirClassOrTypeAliasType, second: CirClassOrTypeAliasType): CirClassOrTypeAliasType? {
        if (first is CirClassType && second is CirClassType) {
            return ClassTypeCommonizer(classifiers).commonize(listOf(first, second))
        }

        if (first is CirTypeAliasType && second is CirTypeAliasType) {
            /*
            In case regular type-alias-type commonization fails, we try to expand all type-aliases and
            try our luck with commonizing those class types
             */
            return TypeAliasTypeCommonizer(classifiers).commonize(listOf(first, second))
                ?: ClassTypeCommonizer(classifiers).commonize(listOf(first.expandedType(), second.expandedType()))
        }

        val classType = when {
            first is CirClassType -> first
            second is CirClassType -> second
            else -> return null
        }

        val typeAliasType = when {
            first is CirTypeAliasType -> first
            second is CirTypeAliasType -> second
            else -> return null
        }

        /*
        TypeAliasCommonizer will be able to figure out if the typealias will be represented as expect class in common.
        If so, re-use this class type, otherwise: try to expand the typeAlias
         */
        val typeAliasClassType = TypeAliasTypeCommonizer(classifiers).commonize(listOf(typeAliasType))?.expandedType()
            ?: typeAliasType.expandedType()

        return ClassTypeCommonizer(classifiers).commonize(listOf(classType, typeAliasClassType))
    }
}

internal tailrec fun CirClassOrTypeAliasType.expandedType(): CirClassType = when (this) {
    is CirClassType -> this
    is CirTypeAliasType -> this.underlyingType.expandedType()
}
