/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.hashCode

interface CirAnnotation {
    val type: CirClassType
    val constantValueArguments: Map<CirName, CirConstantValue>
    val annotationValueArguments: Map<CirName, CirAnnotation>

    companion object {
        fun createInterned(
            type: CirClassType,
            constantValueArguments: Map<CirName, CirConstantValue>,
            annotationValueArguments: Map<CirName, CirAnnotation>
        ): CirAnnotation = interner.intern(
            CirAnnotationInternedImpl(
                type = type,
                constantValueArguments = constantValueArguments,
                annotationValueArguments = annotationValueArguments
            )
        )

        private val interner = Interner<CirAnnotationInternedImpl>()
    }
}

private data class CirAnnotationInternedImpl(
    override val type: CirClassType,
    override val constantValueArguments: Map<CirName, CirConstantValue>,
    override val annotationValueArguments: Map<CirName, CirAnnotation>
) : CirAnnotation {
    private var hashCode = hashCode(type)
        .appendHashCode(constantValueArguments)
        .appendHashCode(annotationValueArguments)


    override fun hashCode(): Int = hashCode

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is CirAnnotationInternedImpl && other.hashCode != this.hashCode) return false
        if (other !is CirAnnotation) return false
        if (other.type != this.type) return false
        if (other.constantValueArguments != this.constantValueArguments) return false
        if (other.annotationValueArguments != this.annotationValueArguments) return false
        return true
    }
}
