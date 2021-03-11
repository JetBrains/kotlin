/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.hints

import com.intellij.codeInsight.hints.ImmediateConfigurable
import org.jetbrains.kotlin.idea.KotlinBundle

@Suppress("UnstableApiUsage")
class KotlinLambdasHintsProvider : KotlinAbstractHintsProvider<KotlinLambdasHintsProvider.Settings>() {

    data class Settings(
        var returnExpressions: Boolean = true,
        var implicitReceiversAndParams: Boolean = true,
    )

    override val name: String = KotlinBundle.message("hints.settings.lambdas")
    override val hintsArePlacedAtTheEndOfLine = true

    override fun isElementSupported(resolved: HintType?, settings: Settings): Boolean {
        return when (resolved) {
            HintType.LAMBDA_RETURN_EXPRESSION -> settings.returnExpressions
            HintType.LAMBDA_IMPLICIT_PARAMETER_RECEIVER -> settings.implicitReceiversAndParams
            else -> false
        }
    }

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return createLambdaHintsImmediateConfigurable(settings)
    }

    override fun createSettings(): Settings = Settings()

    override val previewText: String? = """
        val lambda = { i: Int ->
            i + 10
            i + 20
        }

        fun someFun() {    
            GlobalScope.launch {
                // someSuspendingFun()
            }
        }
    """.trimIndent()
}