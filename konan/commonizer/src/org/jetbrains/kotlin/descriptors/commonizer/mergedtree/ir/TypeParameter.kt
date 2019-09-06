/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.Variance

interface TypeParameter {
    val annotations: Annotations
    val name: Name
    val isReified: Boolean
    val variance: Variance
    val upperBounds: List<UnwrappedType>
}

interface DeclarationWithTypeParameters : Declaration {
    val typeParameters: List<TypeParameter>
}

data class CommonTypeParameter(
    override val name: Name,
    override val isReified: Boolean,
    override val variance: Variance,
    override val upperBounds: List<UnwrappedType>
) : TypeParameter {
    override val annotations: Annotations get() = Annotations.EMPTY
}

data class TargetTypeParameter(private val descriptor: TypeParameterDescriptor) : TypeParameter {
    override val annotations: Annotations get() = descriptor.annotations
    override val name: Name get() = descriptor.name
    override val isReified: Boolean get() = descriptor.isReified
    override val variance: Variance get() = descriptor.variance
    override val upperBounds: List<UnwrappedType> get() = descriptor.upperBounds.map { it.unwrap() }
}
