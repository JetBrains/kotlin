/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.compiler.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtAnnotationEntry

object FirJsoErrors {
    val NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED by error1<PsiElement, String>()
    val ONLY_INTERFACES_ARE_SUPPORTED by error1<PsiElement, String>()
    val IMPLEMENTING_OF_JSO_IS_NOT_SUPPORTED by error1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultErrorMessagesJso)
    }
}
