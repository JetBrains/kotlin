/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.warning1

object PluginErrors {
    val FUNCTION_WITH_DUMMY_NAME by warning1<PsiElement, String>(SourceElementPositioningStrategies.DECLARATION_NAME)
    val ILLEGAL_NUMBER_SIGN by error2<PsiElement, String, String>()
}
