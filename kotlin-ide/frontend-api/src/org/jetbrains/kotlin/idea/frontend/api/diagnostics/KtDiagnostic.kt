/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.diagnostics

import com.intellij.openapi.util.TextRange

interface KtDiagnostic {
    val factoryName: String?
    val message: String
    val textRanges: Collection<TextRange>
    val isValid: Boolean get() = true
}

fun KtDiagnostic.getMessageWithFactoryName(): String =
    if (factoryName == null) message
    else "[$factoryName] $message"

data class KtSimpleDiagnostic(
    override val factoryName: String?,
    override val message: String,
    override val textRanges: Collection<TextRange>,
) : KtDiagnostic