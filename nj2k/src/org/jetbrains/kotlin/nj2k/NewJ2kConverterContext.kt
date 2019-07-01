/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.*

data class NewJ2kConverterContext(
    val symbolProvider: JKSymbolProvider,
    val converter: NewJavaToKotlinConverter,
    val inConversionContext: (PsiElement) -> Boolean,
    val importStorage: ImportStorage,
    val elementsInfoStorage: JKElementInfoStorage
) : ConverterContext {
    val project: Project
        get() = converter.project
}

