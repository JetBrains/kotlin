/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea.quickfixes

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.util.ImportInsertHelperImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.SerializationErrors
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations

internal class AddKotlinxSerializationTransientImportQuickFix(expression: PsiElement) :
    KotlinQuickFixAction<PsiElement>(expression) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        // Do not use Helper itself because it does not insert import on conflict;
        // so it will always fail because k.jvm.Transient is always in auto-import
        ImportInsertHelperImpl.addImport(project, file, SerializationAnnotations.serialTransientFqName, allUnder = false)
    }

    override fun getFamilyName(): String = text

    override fun getText(): String = "Import ${SerializationAnnotations.serialTransientFqName}"

    object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            if (diagnostic.factory != SerializationErrors.INCORRECT_TRANSIENT) return null
            val castedDiagnostic = SerializationErrors.INCORRECT_TRANSIENT.cast(diagnostic)

            val element = castedDiagnostic.psiElement

            return AddKotlinxSerializationTransientImportQuickFix(element)
        }
    }
}