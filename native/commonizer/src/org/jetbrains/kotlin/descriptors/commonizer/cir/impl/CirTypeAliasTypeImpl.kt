/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassOrTypeAliasType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeAliasType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.name.ClassId

data class CirTypeAliasTypeImpl(
    override val classifierId: ClassId,
    override val underlyingType: CirClassOrTypeAliasType,
    override val arguments: List<CirTypeProjection>,
    override val isMarkedNullable: Boolean
) : CirTypeAliasType() {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(classifierId)
        .appendHashCode(underlyingType)
        .appendHashCode(arguments)
        .appendHashCode(isMarkedNullable)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is CirTypeAliasType -> {
            classifierId == other.classifierId
                    && underlyingType == other.underlyingType
                    && isMarkedNullable == other.isMarkedNullable
                    && arguments == other.arguments
        }
        else -> false
    }
}
