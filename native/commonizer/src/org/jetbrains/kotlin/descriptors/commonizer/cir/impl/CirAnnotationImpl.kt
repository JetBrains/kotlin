/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode

data class CirAnnotationImpl(
    override val type: CirClassType,
    override val constantValueArguments: Map<CirName, CirConstantValue<*>>,
    override val annotationValueArguments: Map<CirName, CirAnnotation>
) : CirAnnotation {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(type)
        .appendHashCode(constantValueArguments)
        .appendHashCode(annotationValueArguments)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        return other is CirAnnotation
                && type == other.type
                && constantValueArguments == other.constantValueArguments
                && annotationValueArguments == other.annotationValueArguments
    }
}
