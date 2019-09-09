/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.types.SimpleType
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface TypeAlias : AnnotatedDeclaration, NamedDeclaration, DeclarationWithTypeParameters, DeclarationWithVisibility {
    val underlyingType: SimpleType
    val expandedType: SimpleType
}

class TargetTypeAlias(private val descriptor: TypeAliasDescriptor) : TypeAlias {
    override val annotations get() = descriptor.annotations
    override val name get() = descriptor.name
    override val typeParameters by lazy(PUBLICATION) { descriptor.declaredTypeParameters.map(::TargetTypeParameter) }
    override val visibility get() = descriptor.visibility
    override val underlyingType get() = descriptor.underlyingType
    override val expandedType get() = descriptor.expandedType
}
