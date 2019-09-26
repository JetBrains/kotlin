/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.types.SimpleType
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirTypeAlias : CirAnnotatedDeclaration, CirNamedDeclaration, CirDeclarationWithTypeParameters, CirDeclarationWithVisibility {
    val underlyingType: SimpleType
    val expandedType: SimpleType
}

class CirWrappedTypeAlias(private val wrapped: TypeAliasDescriptor) : CirTypeAlias {
    override val annotations get() = wrapped.annotations
    override val name get() = wrapped.name
    override val typeParameters by lazy(PUBLICATION) { wrapped.declaredTypeParameters.map(::CirWrappedTypeParameter) }
    override val visibility get() = wrapped.visibility
    override val underlyingType get() = wrapped.underlyingType
    override val expandedType get() = wrapped.expandedType
}
