/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.codeInliner.TypeAliasUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.psi.KtTypeAlias

class KotlinInlineTypeAliasProcessor(
    declaration: KtTypeAlias,
    reference: PsiReference?,
    inlineThisOnly: Boolean,
    deleteAfter: Boolean,
    editor: Editor?,
) : AbstractKotlinInlineDeclarationProcessor<KtTypeAlias>(
    declaration = declaration,
    reference = reference,
    inlineThisOnly = inlineThisOnly,
    deleteAfter = deleteAfter,
    editor = editor
) {
    override fun postAction() {
        performDelayedRefactoringRequests(myProject)
    }

    override fun createReplacementStrategy(): UsageReplacementStrategy? = TypeAliasUsageReplacementStrategy(declaration)
}
