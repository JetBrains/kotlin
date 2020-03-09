/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*

class CirAnnotation private constructor(original: AnnotationDescriptor) {
    val fqName: FqName = original.fqName?.intern() ?: error("Annotation with no FQ name: ${original::class.java}, $original")
    val allValueArguments: Map<Name, ConstantValue<*>> = original.allValueArguments.mapKeys { it.key.intern() }

    init {
        allValueArguments.forEach { (name, constantValue) ->
            checkSupportedInCommonization(constantValue) { "${original::class.java}, $original[$name]" }
        }
    }

    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(fqName).appendHashCode(allValueArguments)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is CirAnnotation -> fqName == other.fqName && allValueArguments == other.allValueArguments
        else -> false
    }

    companion object {
        private val interner = Interner<CirAnnotation>()

        fun create(original: AnnotationDescriptor): CirAnnotation = interner.intern(CirAnnotation(original))
    }
}

internal fun checkSupportedInCommonization(constantValue: ConstantValue<*>, location: () -> String) {
    @Suppress("TrailingComma")
    return when (constantValue) {
        is StringValue,
        is IntegerValueConstant<*>,
        is UnsignedValueConstant<*>,
        is BooleanValue,
        is NullValue,
        is DoubleValue,
        is FloatValue,
        is EnumValue -> {
            // OK
        }
        is ArrayValue -> {
            constantValue.value.forEachIndexed { index, innerConstantValue ->
                checkSupportedInCommonization(innerConstantValue) { "${location()}[$index]" }
            }
        }
        else -> error("Unsupported const value type: ${constantValue::class.java}, $constantValue at ${location()}")
    }
}
