/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.KmTypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.cir.unabbreviate
import org.jetbrains.kotlin.descriptors.commonizer.metadata.decodeVisibility
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirTypeAliasFactory {
    fun create(name: CirName, source: KmTypeAlias, typeResolver: CirTypeResolver): CirTypeAlias {
        val underlyingType = CirTypeFactory.create(source.underlyingType, typeResolver) as CirClassOrTypeAliasType
        val expandedType = underlyingType.unabbreviate()

        return CirTypeAlias.create(
            annotations = CirAnnotationFactory.createAnnotations(source.flags, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { CirTypeParameterFactory.create(it, typeResolver) },
            visibility = decodeVisibility(source.flags),
            underlyingType = underlyingType,
            expandedType = expandedType
        )
    }
}
