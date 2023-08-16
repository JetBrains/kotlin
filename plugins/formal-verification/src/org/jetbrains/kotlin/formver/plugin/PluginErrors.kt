/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object PluginErrors {
    val FUNCTION_WITH_UNVERIFIED_CONTRACT by warning1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VIPER_ERROR by warning1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VIPER_TEXT by info2<PsiElement, String, String>(SourceElementPositioningStrategies.DECLARATION_NAME)

    init {
        RootDiagnosticRendererFactory.registerFactory(FormalVerificationPluginErrorMessages)
    }
}