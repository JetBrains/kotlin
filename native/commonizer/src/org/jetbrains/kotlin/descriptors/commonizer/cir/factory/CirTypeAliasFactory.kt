/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirTypeAliasImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern

object CirTypeAliasFactory {
    fun create(source: TypeAliasDescriptor): CirTypeAlias {
        return CirTypeAliasImpl(
            annotations = source.annotations.map(CirAnnotationFactory::create),
            name = source.name.intern(),
            typeParameters = source.declaredTypeParameters.map(CirTypeParameterFactory::create),
            visibility = source.visibility,
            underlyingType = CirTypeFactory.create(source.underlyingType),
            expandedType = CirTypeFactory.create(source.expandedType)
        )
    }
}
