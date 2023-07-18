/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.info1
import org.jetbrains.kotlin.diagnostics.warning0

object PluginErrors {
    val FUNCTION_WITH_UNVERIFIED_CONTRACT by warning0<PsiElement>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val VIPER_TEXT by info1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
}