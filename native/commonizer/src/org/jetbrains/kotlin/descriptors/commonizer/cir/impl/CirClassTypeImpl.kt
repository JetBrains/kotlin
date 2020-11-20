/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.name.ClassId

data class CirClassTypeImpl(
    override val classifierId: ClassId,
    override val outerType: CirClassType?,
    override val visibility: DescriptorVisibility, // visibility of the class descriptor
    override val arguments: List<CirTypeProjection>,
    override val isMarkedNullable: Boolean,
) : CirClassType() {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(classifierId)
        .appendHashCode(outerType)
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

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is CirClassType -> {
            classifierId == other.classifierId
                    && isMarkedNullable == other.isMarkedNullable
                    && visibility == other.visibility
                    && arguments == other.arguments
                    && outerType == other.outerType
        }
        else -> false
    }
}
