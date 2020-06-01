/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirClassFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeAliasFactory
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassifiersCache
import org.jetbrains.kotlin.name.Name

/**
 * Main (optimistic) branch:
 * - Make sure that all TAs are identical, so the resulting TA can be lifted up into "common" fragment.
 *
 * Secondary (backup) branch:
 * - Produce an "expect class" for "common" fragment and the corresponding "actual typealias" declarations for each platform fragment.
 */
class TypeAliasCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirTypeAlias, CirClassifier>() {
    private val main = TypeAliasLiftingUpCommonizer(cache)
    private val secondary = TypeAliasExpectClassCommonizer()

    override fun commonizationResult(): CirClassifier = main.resultOrNull ?: secondary.result

    override fun initialize(first: CirTypeAlias) = Unit

    @Suppress("ReplaceNegatedIsEmptyWithIsNotEmpty")
    override fun doCommonizeWith(next: CirTypeAlias): Boolean {
        val mainResult = main.commonizeWith(next)
        val secondaryResult = secondary.commonizeWith(next)

        // Note: don't call commonizeWith() functions in return statement to avoid short-circuiting!
        return mainResult || secondaryResult
    }
}

private class TypeAliasLiftingUpCommonizer(cache: CirClassifiersCache) : AbstractStandardCommonizer<CirTypeAlias, CirTypeAlias>() {
    private lateinit var name: Name
    private val typeParameters = TypeParameterListCommonizer(cache)
    private val underlyingType = TypeCommonizer(cache)
    private lateinit var expandedType: CirSimpleType
    private val visibility = VisibilityCommonizer.lowering(allowPrivate = false)

    override fun commonizationResult(): CirTypeAlias = CirTypeAliasFactory.create(
        annotations = emptyList(),
        name = name,
        typeParameters = typeParameters.result,
        visibility = visibility.result,
        underlyingType = underlyingType.result as CirSimpleType,
        expandedType = expandedType
    )

    val resultOrNull: CirTypeAlias?
        get() = if (hasResult) commonizationResult() else null

    override fun initialize(first: CirTypeAlias) {
        name = first.name
        expandedType = first.expandedType
    }

    override fun doCommonizeWith(next: CirTypeAlias) =
        typeParameters.commonizeWith(next.typeParameters)
                && underlyingType.commonizeWith(next.underlyingType)
                && visibility.commonizeWith(next)
}

private class TypeAliasExpectClassCommonizer : AbstractStandardCommonizer<CirTypeAlias, CirClass>() {
    private lateinit var name: Name
    private val classVisibility = VisibilityCommonizer.equalizing()

    override fun commonizationResult(): CirClass = CirClassFactory.create(
        annotations = emptyList(),
        name = name,
        typeParameters = emptyList(),
        visibility = classVisibility.result,
        modality = Modality.FINAL,
        kind = ClassKind.CLASS,
        companion = null,
        isCompanion = false,
        isData = false,
        isInline = false,
        isInner = false,
        isExternal = false,
        supertypes = mutableListOf()
    )

    override fun initialize(first: CirTypeAlias) {
        name = first.name
    }

    override fun doCommonizeWith(next: CirTypeAlias) =
        next.typeParameters.isEmpty() // TAs with declared type parameters can't be commonized
                && next.underlyingType.arguments.isEmpty() // TAs with functional types or types with parameters at the right-hand side can't be commonized
                && next.underlyingType.kind == CirSimpleTypeKind.CLASS // right-hand side could have only class
                && classVisibility.commonizeWith(next.underlyingType) // the visibilities of the right-hand classes should be equal
}
