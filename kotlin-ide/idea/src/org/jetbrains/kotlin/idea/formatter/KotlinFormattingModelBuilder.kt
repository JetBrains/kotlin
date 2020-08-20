/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class KotlinFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val element = formattingContext.psiElement
        val containingFile = formattingContext.containingFile
        val block = KotlinBlock(
            containingFile.node,
            NodeAlignmentStrategy.getNullStrategy(),
            Indent.getNoneIndent(),
            wrap = null,
            settings,
            createSpacingBuilder(settings, KotlinSpacingBuilderUtilImpl)
        )

        if (element is PsiFile) {
            val collectChangesModel = createCollectFormattingChangesModel(element, block)
            if (collectChangesModel != null) {
                return collectChangesModel
            }
        }

        return FormattingModelProvider.createFormattingModelForPsiFile(containingFile, block, settings)
    }

    override fun getRangeAffectingIndent(psiFile: PsiFile, i: Int, astNode: ASTNode): TextRange? {
        return null
    }
}
