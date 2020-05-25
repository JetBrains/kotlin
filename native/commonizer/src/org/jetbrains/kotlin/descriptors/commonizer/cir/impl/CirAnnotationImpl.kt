/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ConstantValue

data class CirAnnotationImpl(
    override val fqName: FqName,
    override val constantValueArguments: Map<Name, ConstantValue<*>>,
    override val annotationValueArguments: Map<Name, CirAnnotation>
) : CirAnnotation {
    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(fqName)
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
                && fqName == other.fqName
                && constantValueArguments == other.constantValueArguments
                && annotationValueArguments == other.annotationValueArguments
    }
}
