/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.JvmPsiConversionHelper
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType

internal class ParameterInfo(
    val name: String,
    val type: PsiType,
    val annotations: List<PsiAnnotation>,
)

internal fun PsiMethod.getParametersInfo(): List<ParameterInfo> {
    val typeConverter = JvmPsiConversionHelper.getInstance(project)
    return this.parameterList.parameters.map {
        ParameterInfo(it.name, typeConverter.convertType(it.type), it.annotations.asList())
    }
}
