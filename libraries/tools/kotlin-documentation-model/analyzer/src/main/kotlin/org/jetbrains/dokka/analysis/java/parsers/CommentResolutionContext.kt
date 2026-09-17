/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.dokka.analysis.java.JavadocTag

internal data class CommentResolutionContext(
    val comment: PsiDocComment,
    val tag: JavadocTag?
)
