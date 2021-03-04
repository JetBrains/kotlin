/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirTypeAliasImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirTypeAliasFactory {
    fun create(source: TypeAliasDescriptor): CirTypeAlias {
        val underlyingType = CirTypeFactory.create(source.underlyingType) as CirClassOrTypeAliasType
        val expandedType = CirTypeFactory.unabbreviate(underlyingType)

        return create(
            annotations = source.annotations.compactMap(CirAnnotationFactory::create),
            name = CirName.create(source.name),
            typeParameters = source.declaredTypeParameters.compactMap(CirTypeParameterFactory::create),
            visibility = source.visibility,
            underlyingType = underlyingType,
            expandedType = expandedType
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        name: CirName,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        underlyingType: CirClassOrTypeAliasType,
        expandedType: CirClassType
    ): CirTypeAlias {
        return CirTypeAliasImpl(
            annotations = annotations,
            name = name,
            typeParameters = typeParameters,
            visibility = visibility,
            underlyingType = underlyingType,
            expandedType = expandedType
        )
    }
}
