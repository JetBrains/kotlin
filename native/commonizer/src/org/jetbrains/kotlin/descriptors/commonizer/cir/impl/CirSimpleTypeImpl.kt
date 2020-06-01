/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirSimpleType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirSimpleTypeKind
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.name.FqName

data class CirSimpleTypeImpl(
    override val kind: CirSimpleTypeKind,
    override val visibility: Visibility, // visibility of the classifier descriptor
    override val fqName: FqName,
    override val arguments: List<CirTypeProjection>,
    override val isMarkedNullable: Boolean,
    override val isDefinitelyNotNullType: Boolean,
    override val fqNameWithTypeParameters: String
) : CirSimpleType() {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(kind)
        .appendHashCode(visibility)
        .appendHashCode(fqName)
        .appendHashCode(arguments)
        .appendHashCode(isMarkedNullable)
        .appendHashCode(isDefinitelyNotNullType)
        .appendHashCode(fqNameWithTypeParameters)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?) = when {
        other === this -> true
        other is CirSimpleType -> {
            isMarkedNullable == other.isMarkedNullable
                    && fqName == other.fqName
                    && kind == other.kind
                    && visibility == other.visibility
                    && arguments == other.arguments
                    && fqNameWithTypeParameters == other.fqNameWithTypeParameters
                    && isDefinitelyNotNullType == other.isDefinitelyNotNullType
        }
        else -> false
    }
}
