/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.export.utilities

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val jvmNameFqn = ClassId.topLevel(FqName("kotlin.jvm.JvmName"))

public fun KaCallableSymbol.getJvmNameOrNull(): String? {
    return if (annotations.any { it.classId == jvmNameFqn }) {
        val annotations = annotations.firstOrNull { it.classId == jvmNameFqn } ?: return null
        val arguments = annotations.arguments.getOrNull(0) ?: return null
        (arguments.expression as KaAnnotationValue.ConstantValue).value.value as String
    } else null
}