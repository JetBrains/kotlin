/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import kotlin.reflect.KClass

interface KtDiagnostic : ValidityTokenOwner {
    val factoryName: String?
    val defaultMessage: String
}

interface KtDiagnosticWithPsi : KtDiagnostic {
    val psi: PsiElement
    val textRanges: Collection<TextRange>
    val diagnosticClass: KClass<out KtDiagnosticWithPsi>
}

class KtSimpleDiagnostic(
    override val factoryName: String?,
    override val defaultMessage: String,
    override val token: ValidityToken,
) : KtDiagnostic

fun KtDiagnostic.getDefaultMessageWithFactoryName(): String =
    if (factoryName == null) defaultMessage
    else "[$factoryName] $defaultMessage"