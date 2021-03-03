/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea.quickfixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.SerializationErrors

internal class JsonRedundantDefaultQuickFix(expression: KtCallExpression) : KotlinQuickFixAction<KtCallExpression>(expression) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val factory = KtPsiFactory(project)
        val jsonExpr = factory.createExpression("Json")
        element.replace(jsonExpr)
    }

    override fun getFamilyName(): String = text

    override fun getText(): String = KotlinBundle.message("replace.with.0", "Json")

    object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            if (diagnostic.factory != SerializationErrors.JSON_FORMAT_REDUNDANT_DEFAULT) return null
            val castedDiagnostic = SerializationErrors.JSON_FORMAT_REDUNDANT_DEFAULT.cast(diagnostic)

            val element: KtCallExpression = castedDiagnostic.psiElement as? KtCallExpression ?: return null
            return JsonRedundantDefaultQuickFix(element)
        }
    }
}
