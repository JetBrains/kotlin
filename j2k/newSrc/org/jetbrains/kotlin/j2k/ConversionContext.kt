/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.tree.JKElement

data class ConversionContext(
    val symbolProvider: JKSymbolProvider,
    val converter: NewJavaToKotlinConverter,
    val inConversionContext: (PsiElement) -> Boolean
) {

    val project: Project get() = converter.project
}