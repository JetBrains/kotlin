/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue

fun getVisibility(annotation: AnnotationDescriptor, field: String = "value"): DescriptorVisibility {
    val value = annotation.getStringArgument(field) ?: "PUBLIC"
    val visibility = when (value) {
        "PUBLIC" -> Visibilities.Public
        "PROTECTED" -> Visibilities.Protected
        "PRIVATE" -> Visibilities.Private
        "PACKAGE" -> JavaVisibilities.PackageVisibility
        else -> Visibilities.Public
    }
    return JavaDescriptorVisibilities.toDescriptorVisibility(visibility)
}

fun AnnotationDescriptor.getStringArgument(argumentName: String): String? {
    val argument = allValueArguments[Name.identifier(argumentName)]
        ?: return null

    return when (argument) {
        is EnumValue -> argument.enumEntryName.identifier
        is StringValue -> argument.value
        else -> throw RuntimeException("Argument $argument is not supported")
    }
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
