/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode

data class CirSimpleTypeImpl(
    override val classifierId: CirClassifierId,
    override val visibility: Visibility, // visibility of the classifier descriptor
    override val arguments: List<CirTypeProjection>,
    override val isMarkedNullable: Boolean
) : CirSimpleType() {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(classifierId)
        .appendHashCode(visibility)
        .appendHashCode(arguments)
        .appendHashCode(isMarkedNullable)

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
                    && classifierId == other.classifierId
                    && visibility == other.visibility
                    && arguments == other.arguments
        }
        else -> false
    }
}
