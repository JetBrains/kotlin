/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor

interface CirTypeAlias : CirAnnotatedDeclaration, CirNamedDeclaration, CirDeclarationWithTypeParameters, CirDeclarationWithVisibility {
    val underlyingType: CirSimpleType
    val expandedType: CirSimpleType
}

class CirTypeAliasImpl(original: TypeAliasDescriptor) : CirTypeAlias {
    override val annotations = original.annotations.map(::CirAnnotation)
    override val name = original.name
    override val typeParameters = original.declaredTypeParameters.map(::CirTypeParameterImpl)
    override val visibility = original.visibility
    override val underlyingType = CirSimpleType(original.underlyingType)
    override val expandedType = CirSimpleType(original.expandedType)
}
