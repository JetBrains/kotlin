/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirTypeAliasType
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

internal class ClassOrTypeAliasTypeStatelessCommonizer(
    private val classifiers: CirKnownClassifiers
) : StatelessCommonizer<CirClassOrTypeAliasType, CirClassOrTypeAliasType> {

    override fun commonize(values: List<CirClassOrTypeAliasType>): CirClassOrTypeAliasType? {
        if (values.isEmpty()) return null
        values.singleOrNull()?.let { return it }

        val classTypes = values.filterIsInstance<CirClassType>()
        val typeAliasTypes = values.filterIsInstance<CirTypeAliasType>()
        val expandedTypeAliasTypes = typeAliasTypes.map { it.expandedType() }

        if (values.all { it is CirClassType }) {
            return ClassTypeCommonizer(classifiers).commonize(classTypes)
        }

        if (values.all { it is CirTypeAliasType }) {
            /*
            In case regular type-alias-type commonization fails, we try to expand all type-aliases and
            try our luck with commonizing those class types
             */
            return TypeAliasTypeCommonizer(classifiers).commonize(typeAliasTypes)
                ?: ClassTypeCommonizer(classifiers).commonize(expandedTypeAliasTypes)
        }

        /*
        There are type-alias types & class types enqueued for commonization.
        We reduce all classes to a common representation and all type aliases to a common representation.
         */
        val commonizedClass = ClassTypeCommonizer(classifiers).commonize(classTypes) ?: return null
        val commonizedTypeAlias = TypeAliasTypeCommonizer(classifiers).commonize(typeAliasTypes)

        if (commonizedTypeAlias != null) {
            return ClassTypeCommonizer(classifiers).commonize(listOf(commonizedClass, commonizedTypeAlias.expandedType()))
        }

        /*
        If type-alias type commonization failed:
        Last attempt: Try to commonize all type-alias expansions with the commonized class
         */
        return ClassTypeCommonizer(classifiers).commonize(expandedTypeAliasTypes + commonizedClass)
    }
}

internal class ClassOrTypeAliasTypeCommonizer(classifiers: CirKnownClassifiers) :
    StatelessCommonizerAdapter<CirClassOrTypeAliasType, CirClassOrTypeAliasType>(ClassOrTypeAliasTypeStatelessCommonizer(classifiers))

internal tailrec fun CirClassOrTypeAliasType.expandedType(): CirClassType = when (this) {
    is CirClassType -> this
    is CirTypeAliasType -> this.underlyingType.expandedType()
}
