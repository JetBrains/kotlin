/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.CommonizerSettings
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

class TypeAliasCommonizer(
    private val classifiers: CirKnownClassifiers,
    private val settings: CommonizerSettings,
    typeCommonizer: TypeCommonizer,
) : NullableSingleInvocationCommonizer<CirTypeAlias> {

    private val typeCommonizer = typeCommonizer.withContext {
        withBackwardsTypeAliasSubstitutionEnabled(false)
    }

    override fun invoke(values: List<CirTypeAlias>): CirTypeAlias? {
        if (values.isEmpty()) return null

        val name = values.map { it.name }.distinct().singleOrNull() ?: return null

        val typeParameters = TypeParameterListCommonizer(typeCommonizer).commonize(values.map { it.typeParameters }) ?: return null

        val underlyingType = typeCommonizer.invoke(values.map { it.underlyingType }) as? CirClassOrTypeAliasType ?: return null

        val visibility = VisibilityCommonizer.lowering().commonize(values) ?: return null

        val unsafeNumberAnnotation = createUnsafeNumberAnnotationIfNecessary(
            classifiers.classifierIndices.targets, settings,
            inputDeclarations = values,
            inputTypes = values.map { it.underlyingType },
            commonizedType = underlyingType,
        )

        return CirTypeAlias.create(
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            underlyingType = underlyingType,
            expandedType = underlyingType.expandedType(),
            annotations = listOfNotNull(unsafeNumberAnnotation),
        )
    }
}
