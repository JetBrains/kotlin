/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.frontend.api.calls.KtCall
import org.jetbrains.uast.UastLanguagePlugin

val firKotlinUastPlugin: UastLanguagePlugin by lz {
    UastLanguagePlugin.getInstances().find { it.language == KotlinLanguage.INSTANCE }
        ?: FirKotlinUastLanguagePlugin()
}

internal fun KtCall.toPsiMethod(): PsiMethod? {
    if (isErrorCall) return null
    val psi = targetFunction.candidates.singleOrNull()?.psi ?: return null
    return psi.getRepresentativeLightMethod()
}
