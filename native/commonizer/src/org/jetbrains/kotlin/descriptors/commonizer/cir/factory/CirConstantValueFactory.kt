/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import kotlinx.metadata.KmAnnotationArgument
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapIndexed
import org.jetbrains.kotlin.resolve.constants.*

object CirConstantValueFactory {
    fun createSafely(
        constantValue: ConstantValue<*>,
        constantName: CirName? = null,
        owner: Any,
    ): CirConstantValue<*> = createSafely(
        constantValue = constantValue,
        location = { "${owner::class.java}, $owner" + constantName?.toString()?.let { "[$it]" } }
    )

    private fun createSafely(
        constantValue: ConstantValue<*>,
        location: () -> String
    ): CirConstantValue<*> = when (constantValue) {
        is NullValue -> CirConstantValue.NullValue

        is StringValue -> CirConstantValue.StringValue(constantValue.value)
        is CharValue -> CirConstantValue.CharValue(constantValue.value)

        is ByteValue -> CirConstantValue.ByteValue(constantValue.value)
        is ShortValue -> CirConstantValue.ShortValue(constantValue.value)
        is IntValue -> CirConstantValue.IntValue(constantValue.value)
        is LongValue -> CirConstantValue.LongValue(constantValue.value)

        is UByteValue -> CirConstantValue.UByteValue(constantValue.value)
        is UShortValue -> CirConstantValue.UShortValue(constantValue.value)
        is UIntValue -> CirConstantValue.UIntValue(constantValue.value)
        is ULongValue -> CirConstantValue.ULongValue(constantValue.value)

        is FloatValue -> CirConstantValue.FloatValue(constantValue.value)
        is DoubleValue -> CirConstantValue.DoubleValue(constantValue.value)
        is BooleanValue -> CirConstantValue.BooleanValue(constantValue.value)

        is EnumValue -> CirConstantValue.EnumValue(
            CirEntityId.create(constantValue.enumClassId),
            CirName.create(constantValue.enumEntryName)
        )

        is ArrayValue -> CirConstantValue.ArrayValue(
            constantValue.value.compactMapIndexed { index, innerConstantValue ->
                createSafely(
                    constantValue = innerConstantValue,
                    location = { "${location()}[$index]" }
                )
            }
        )

        else -> error("Unsupported const value type: ${constantValue::class.java}, $constantValue at ${location()}")
    }

    fun createSafely(
        constantValue: KmAnnotationArgument<*>?,
        constantName: CirName? = null,
        owner: Any,
    ): CirConstantValue<*> = createSafely(
        constantValue = constantValue,
        location = { "${owner::class.java}, $owner" + constantName?.toString()?.let { "[$it]" } }
    )

    private fun createSafely(
        constantValue: KmAnnotationArgument<*>?,
        location: () -> String
    ): CirConstantValue<*> = when (constantValue) {
        null -> CirConstantValue.NullValue

        is KmAnnotationArgument.StringValue -> CirConstantValue.StringValue(constantValue.value)
        is KmAnnotationArgument.CharValue -> CirConstantValue.CharValue(constantValue.value)

        is KmAnnotationArgument.ByteValue -> CirConstantValue.ByteValue(constantValue.value)
        is KmAnnotationArgument.ShortValue -> CirConstantValue.ShortValue(constantValue.value)
        is KmAnnotationArgument.IntValue -> CirConstantValue.IntValue(constantValue.value)
        is KmAnnotationArgument.LongValue -> CirConstantValue.LongValue(constantValue.value)

        is KmAnnotationArgument.UByteValue -> CirConstantValue.UByteValue(constantValue.value)
        is KmAnnotationArgument.UShortValue -> CirConstantValue.UShortValue(constantValue.value)
        is KmAnnotationArgument.UIntValue -> CirConstantValue.UIntValue(constantValue.value)
        is KmAnnotationArgument.ULongValue -> CirConstantValue.ULongValue(constantValue.value)

        is KmAnnotationArgument.FloatValue -> CirConstantValue.FloatValue(constantValue.value)
        is KmAnnotationArgument.DoubleValue -> CirConstantValue.DoubleValue(constantValue.value)
        is KmAnnotationArgument.BooleanValue -> CirConstantValue.BooleanValue(constantValue.value)

        is KmAnnotationArgument.EnumValue -> CirConstantValue.EnumValue(
            CirEntityId.create(constantValue.enumClassName),
            CirName.create(constantValue.enumEntryName)
        )

        is KmAnnotationArgument.ArrayValue -> CirConstantValue.ArrayValue(
            constantValue.value.compactMapIndexed { index, innerConstantValue ->
                createSafely(
                    constantValue = innerConstantValue,
                    location = { "${location()}[$index]" }
                )
            }
        )

        else -> error("Unsupported annotation argument type: ${constantValue::class.java}, $constantValue")
    }
}
