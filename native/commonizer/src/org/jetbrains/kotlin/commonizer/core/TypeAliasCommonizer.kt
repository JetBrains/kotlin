/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

class TypeAliasCommonizer(
    private val classifiers: CirKnownClassifiers
) : AssociativeCommonizer<CirTypeAlias> {

    override fun commonize(first: CirTypeAlias, second: CirTypeAlias): CirTypeAlias? {
        val name = if (first.name == second.name) first.name else return null

        val typeParameters = TypeParameterListCommonizer(classifiers)
            .commonize(listOf(first.typeParameters, second.typeParameters)) ?: return null

        val underlyingType = TypeCommonizer(classifiers)
            .commonize(first.underlyingType, second.underlyingType) as? CirClassOrTypeAliasType ?: return null

        val visibility = VisibilityCommonizer.lowering().commonize(listOf(first, second)) ?: return null

        return CirTypeAlias.create(
            annotations = emptyList(),
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            underlyingType = underlyingType,
            expandedType = underlyingType.expandedType()
        )
    }
}