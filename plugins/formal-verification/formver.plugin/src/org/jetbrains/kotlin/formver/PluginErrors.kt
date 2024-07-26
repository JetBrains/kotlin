/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

object PluginErrors {
    val VIPER_VERIFICATION_ERROR by warning1<PsiElement, String>()
    val VIPER_TEXT by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INTERNAL_ERROR by error1<PsiElement, String>()
    val MINOR_INTERNAL_ERROR by error1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val UNEXPECTED_RETURNED_VALUE by warning1<PsiElement, String>()
    val INVALID_INVOCATION_TYPE by warning2<PsiElement, FirBasedSymbol<*>, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FormalVerificationPluginErrorMessages)
    }
}