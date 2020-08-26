/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.findUsages.DescriptiveNameUtil
import com.intellij.openapi.project.Project
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*

abstract class AbstractKotlinDeclarationInlineProcessor<TElement : KtDeclaration>(
    protected val declaration: TElement,
    project: Project,
) : BaseRefactoringProcessor(project) {

    protected val kind = when (declaration) {
        is KtNamedFunction ->
            if (declaration.name != null) KotlinBundle.message("text.function")
            else KotlinBundle.message("text.anonymous.function")

        is KtProperty ->
            if (declaration.isLocal)
                KotlinBundle.message("text.local.variable")
            else
                KotlinBundle.message("text.local.property")

        is KtTypeAlias -> KotlinBundle.message("text.type.alias")
        else -> KotlinBundle.message("text.declaration")
    }

    override fun getCommandName(): String = KotlinBundle.message(
        "text.inlining.0.1",
        kind,
        DescriptiveNameUtil.getDescriptiveName(declaration)
    )

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor = object : UsageViewDescriptor {
        override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) = RefactoringBundle.message(
            "comments.elements.header",
            UsageViewBundle.getOccurencesString(usagesCount, filesCount),
        )

        override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) = RefactoringBundle.message(
            "invocations.to.be.inlined",
            UsageViewBundle.getReferencesString(usagesCount, filesCount),
        )

        override fun getElements() = arrayOf<KtDeclaration>(declaration)

        override fun getProcessedElementsHeader() = KotlinBundle.message("text.0.to.inline", kind.capitalize())
    }
}