/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object PluginErrors {
    val VIPER_VERIFICATION_ERROR by warning1<PsiElement, String>()
    val VIPER_TEXT by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val EXP_EMBEDDING by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val INTERNAL_ERROR by error1<PsiElement, String>()
    val MINOR_INTERNAL_ERROR by error1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val UNEXPECTED_RETURNED_VALUE by warning1<PsiElement, String>()
    val CONDITIONAL_EFFECT_ERROR by warning2<PsiElement, String, String>()
    val POSSIBLE_INDEX_OUT_OF_BOUND by warning2<PsiElement, String, String>()
    val INVALID_SUBLIST_RANGE by warning2<PsiElement, String, String>()

    val UNIQUENESS_VIOLATION by error1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FormalVerificationPluginErrorMessages)
    }
}