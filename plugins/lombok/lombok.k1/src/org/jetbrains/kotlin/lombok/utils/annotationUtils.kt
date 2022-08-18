/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.lombok.config.AccessLevel
import org.jetbrains.kotlin.lombok.config.toDescriptorVisibility
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*

fun getVisibility(annotation: AnnotationDescriptor, field: String = "value"): DescriptorVisibility =
    getAccessLevel(annotation, field).toDescriptorVisibility()

fun getAccessLevel(annotation: AnnotationDescriptor, field: String = "value"): AccessLevel {
    val value = annotation.getStringArgument(field) ?: return AccessLevel.PUBLIC
    return AccessLevel.valueOf(value)
}

@JvmName("getAccessLevelWithReceiver")
fun AnnotationDescriptor.getAccessLevel(field: String = "value"): AccessLevel {
    return getAccessLevel(this, field)
}

fun AnnotationDescriptor.getStringArgument(argumentName: String): String? {
    val argument = allValueArguments[Name.identifier(argumentName)]
        ?: return null

    return extractString(argument)
}

fun AnnotationDescriptor.getStringArrayArgument(argumentName: String): List<String>? =
    when (val argument = allValueArguments[Name.identifier(argumentName)]) {
        null -> null
        is ArrayValue -> argument.value.map(::extractString)
        else -> throw RuntimeException("Argument should be ArrayValue, got $argument")
    }

private fun extractString(argument: ConstantValue<*>) = when (argument) {
    is EnumValue -> argument.enumEntryName.identifier
    is StringValue -> argument.value
    else -> throw RuntimeException("Argument $argument is not supported")
}

fun AnnotationDescriptor.getNonBlankStringArgument(argumentName: String): String? =
    getStringArgument(argumentName).trimToNull()

fun AnnotationDescriptor.getBooleanArgument(argumentName: String): Boolean? {
    val argument = allValueArguments[Name.identifier(argumentName)]
        ?: return null

    return when (argument) {
        is BooleanValue -> argument.value
        else -> throw RuntimeException("Argument $argument is not supported for Boolean value")
    }
}
