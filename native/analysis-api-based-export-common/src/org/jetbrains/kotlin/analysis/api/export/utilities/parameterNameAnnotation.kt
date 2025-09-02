/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

public fun KaType.getValueFromParameterNameAnnotation(): Name? {
    val resultingAnnotation = findParameterNameAnnotation() ?: return null
    val parameterNameArgument = resultingAnnotation.arguments
        .singleOrNull { it.name == StandardClassIds.Annotations.ParameterNames.parameterNameName }

    val constantArgumentValue = parameterNameArgument?.expression as? KaAnnotationValue.ConstantValue ?: return null

    return (constantArgumentValue.value.value as? String)?.let(Name::identifier)
}

private fun KaType.findParameterNameAnnotation(): KaAnnotation? {
    val allParameterNameAnnotations = annotations[StandardNames.FqNames.parameterNameClassId]
    val (explicitAnnotations, implicitAnnotations) = allParameterNameAnnotations.partition { it.psi != null }

    return explicitAnnotations.firstOrNull() ?: implicitAnnotations.singleOrNull()
}