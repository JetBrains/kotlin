/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.checkConstantSupportedInCommonization
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*

class CirAnnotation private constructor(
    val fqName: FqName,
    val constantValueArguments: Map<Name, ConstantValue<*>>,
    val annotationValueArguments: Map<Name, CirAnnotation>
) {
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

    companion object {
        private val interner = Interner<CirAnnotation>()

        fun create(original: AnnotationDescriptor): CirAnnotation {
            val fqName: FqName = original.fqName?.intern() ?: error("Annotation with no FQ name: ${original::class.java}, $original")

            val allValueArguments: Map<Name, ConstantValue<*>> = original.allValueArguments
            if (allValueArguments.isEmpty())
                return create(fqName = fqName, constantValueArguments = emptyMap(), annotationValueArguments = emptyMap())

            val constantValueArguments: MutableMap<Name, ConstantValue<*>> = HashMap()
            val annotationValueArguments: MutableMap<Name, CirAnnotation> = HashMap()

            allValueArguments.forEach { (name, constantValue) ->
                checkConstantSupportedInCommonization(
                    constantValue = constantValue,
                    constantName = name,
                    owner = original,
                    allowAnnotationValues = true
                )

                if (constantValue is AnnotationValue)
                    annotationValueArguments[name.intern()] = create(original = constantValue.value)
                else
                    constantValueArguments[name.intern()] = constantValue
            }

            return create(
                fqName = fqName,
                constantValueArguments = constantValueArguments,
                annotationValueArguments = annotationValueArguments
            )
        }

        fun create(
            fqName: FqName,
            constantValueArguments: Map<Name, ConstantValue<*>>,
            annotationValueArguments: Map<Name, CirAnnotation>
        ): CirAnnotation {
            return interner.intern(
                CirAnnotation(
                    fqName = fqName,
                    constantValueArguments = constantValueArguments,
                    annotationValueArguments = annotationValueArguments
                )
            )
        }
    }
}
