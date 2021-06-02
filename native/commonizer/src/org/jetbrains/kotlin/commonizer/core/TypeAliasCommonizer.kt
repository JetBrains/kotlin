/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality

/**
 * Primary (optimistic) branch:
 * - Make sure that all TAs expand to the same type, so the resulting TA can be short-circuited and lifted up into "common" fragment.
 *
 * Secondary (less optimistic) branch:
 * - Make sure that all TAs are identical, so the resulting TA can be lifted up into "common" fragment.
 */
class TypeAliasCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirTypeAlias, CirClassifier>() {
    private val primary = TypeAliasShortCircuitingCommonizer(classifiers)
    private val secondary = TypeAliasLiftingUpCommonizer(classifiers)

    override fun commonizationResult(): CirClassifier = primary.resultOrNull ?: secondary.result

    override fun initialize(first: CirTypeAlias) = Unit

    override fun doCommonizeWith(next: CirTypeAlias): Boolean {
        val primaryResult = primary.commonizeWith(next)
        val secondaryResult = secondary.commonizeWith(next)

        // Note: don't call commonizeWith() functions in return statement to avoid short-circuiting!
        return primaryResult || secondaryResult
    }
}

private class TypeAliasShortCircuitingCommonizer(
    private val classifiers: CirKnownClassifiers
) : AbstractStandardCommonizer<CirTypeAlias, CirTypeAlias>() {
    private lateinit var name: CirName
    private val typeParameters = TypeParameterListCommonizer(classifiers)
    private var underlyingType: CirClassOrTypeAliasType? = null // null means not computed yet
    private val expandedType = TypeCommonizer(classifiers)
    private val visibility = VisibilityCommonizer.lowering()

    override fun commonizationResult() = CirTypeAlias.create(
        annotations = emptyList(),
        name = name,
        typeParameters = typeParameters.result,
        visibility = visibility.result,
        underlyingType = underlyingType!!,
        expandedType = expandedType.result as CirClassType
    )

    override fun initialize(first: CirTypeAlias) {
        name = first.name
    }

    override fun doCommonizeWith(next: CirTypeAlias): Boolean {
        if (underlyingType == null) {
            underlyingType = computeSuitableUnderlyingType(classifiers, next.underlyingType) ?: return false
        }

        return typeParameters.commonizeWith(next.typeParameters)
                && expandedType.commonizeWith(next.expandedType)
                && visibility.commonizeWith(next)
    }
}

private class TypeAliasLiftingUpCommonizer(classifiers: CirKnownClassifiers) : AbstractStandardCommonizer<CirTypeAlias, CirTypeAlias>() {
    private lateinit var name: CirName
    private val typeParameters = TypeParameterListCommonizer(classifiers)
    private val underlyingType = TypeCommonizer(classifiers)
    private val visibility = VisibilityCommonizer.lowering()

    override fun commonizationResult(): CirTypeAlias {
        val underlyingType = underlyingType.result as CirClassOrTypeAliasType

        return CirTypeAlias.create(
            annotations = emptyList(),
            name = name,
            typeParameters = typeParameters.result,
            visibility = visibility.result,
            underlyingType = underlyingType,
            expandedType = computeExpandedType(underlyingType)
        )
    }

    override fun initialize(first: CirTypeAlias) {
        name = first.name
    }

    override fun doCommonizeWith(next: CirTypeAlias) =
        typeParameters.commonizeWith(next.typeParameters)
                && underlyingType.commonizeWith(next.underlyingType)
                && visibility.commonizeWith(next)
}
