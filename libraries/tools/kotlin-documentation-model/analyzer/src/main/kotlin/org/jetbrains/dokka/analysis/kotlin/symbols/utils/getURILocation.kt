/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.utils

import com.intellij.psi.PsiElement

/**
 * @return a string in the format `file://file.kt:<line>:<column>`,
 * or `null` if [psiElement] is not from a source file
 */
internal fun getLocation(psiElement: PsiElement): String? {
    val filePath = psiElement.containingFile?.virtualFile?.path ?: return null
    val offset = psiElement.textOffset
    val fileDocument = psiElement.containingFile?.fileDocument
    val lineNumber = fileDocument?.getLineNumber(offset)

    return if (lineNumber != null) {
        val column =  offset - fileDocument.getLineStartOffset(lineNumber)
        "file:///$filePath:${lineNumber + 1}:${column + 1}"
    } else
        "file:///$filePath"
}