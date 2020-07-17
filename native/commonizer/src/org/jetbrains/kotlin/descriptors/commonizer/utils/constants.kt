/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*

internal fun checkConstantSupportedInCommonization(
    constantValue: ConstantValue<*>,
    constantName: Name? = null,
    owner: Any,
    allowAnnotationValues: Boolean,
    onError: (String) -> Nothing = ::error
) {
    checkConstantSupportedInCommonization(
        constantValue = constantValue,
        location = { "${owner::class.java}, $owner" + constantName?.asString()?.let { "[$it]" } },
        allowAnnotationValues = allowAnnotationValues,
        onError = onError
    )
}

private fun checkConstantSupportedInCommonization(
    constantValue: ConstantValue<*>,
    location: () -> String,
    allowAnnotationValues: Boolean,
    onError: (String) -> Nothing
) {
    @Suppress("TrailingComma")
    when (constantValue) {
        is StringValue,
        is IntegerValueConstant<*>,
        is UnsignedValueConstant<*>,
        is BooleanValue,
        is NullValue,
        is DoubleValue,
        is FloatValue,
        is EnumValue -> {
            return // OK
        }
        is AnnotationValue -> {
            if (allowAnnotationValues) {
                if (constantValue.value.fqName?.isUnderStandardKotlinPackages != true)
                    onError("Only ${constantValue::class.java} const values from Kotlin standard packages are supported, $constantValue at ${location()}")
                return // OK
            } // else fail (see below)
        }
        is ArrayValue -> {
            constantValue.value.forEachIndexed { index, innerConstantValue ->
                checkConstantSupportedInCommonization(
                    constantValue = innerConstantValue,
                    location = { "${location()}[$index]" },
                    allowAnnotationValues = allowAnnotationValues,
                    onError = onError
                )
            }
            return // OK
        }
    }

    onError("Unsupported const value type: ${constantValue::class.java}, $constantValue at ${location()}")
}

