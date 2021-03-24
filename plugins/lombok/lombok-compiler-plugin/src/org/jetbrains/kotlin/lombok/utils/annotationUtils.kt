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
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaLiteralAnnotationArgument
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.EnumValue
import java.lang.IllegalArgumentException

internal fun getVisibility(annotation: JavaAnnotation, field: String = "value"): DescriptorVisibility {
    val value = getStringAnnotationValue(annotation, field, "PUBLIC")
    val visibility = when (value) {
        "PUBLIC" -> Visibilities.Public
        "PROTECTED" -> Visibilities.Protected
        "PRIVATE" -> Visibilities.Private
        "PACKAGE" -> JavaVisibilities.PackageVisibility
        else -> Visibilities.Public
    }
    return DescriptorVisibilities.toDescriptorVisibility(visibility)
}

internal fun getVisibility(annotation: AnnotationDescriptor, field: String = "value"): DescriptorVisibility {
    val value = getStringAnnotationValue(annotation, field, "PUBLIC")
    val visibility = when (value) {
        "PUBLIC" -> Visibilities.Public
        "PROTECTED" -> Visibilities.Protected
        "PRIVATE" -> Visibilities.Private
        "PACKAGE" -> JavaVisibilities.PackageVisibility
        else -> Visibilities.Public
    }
    return DescriptorVisibilities.toDescriptorVisibility(visibility)
}

private fun getStringAnnotationValue(annotation: JavaAnnotation, argumentName: String, default: String): String {
    val argument = annotation.arguments.find { it.name?.identifier == argumentName }
        ?: throw IllegalArgumentException("No argument '$argumentName' found in $annotation")

    return when (argument) {
        is JavaEnumValueAnnotationArgument -> argument.entryName?.asString() ?: default
        is JavaLiteralAnnotationArgument -> argument.value?.toString() ?: default
        else -> throw RuntimeException("Argument $argument is not supported")
    }
}

private fun getStringAnnotationValue(annotation: AnnotationDescriptor, argumentName: String, default: String): String {
    val argument = annotation.allValueArguments[Name.identifier(argumentName)]
        ?: return default

    return when (argument) {
        is EnumValue -> argument.enumEntryName.identifier
        else -> throw RuntimeException("Argument $argument is not supported")
    }
}
