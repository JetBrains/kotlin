/*
 * Copyright 2010-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.Language
import com.intellij.lang.findUsages.DescriptiveNameUtil
import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.OverrideMethodsProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.inline.GenericInlineHandler
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.replaceUsages
import org.jetbrains.kotlin.idea.findUsages.ReferencesSearchScopeHelper
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.pullUp.deleteWithCompanion
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.declarationsSearch.findSuperMethodsNoWrapping
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.psi.*

private val LOG = Logger.getInstance(AbstractKotlinInlineDeclarationProcessor::class.java)

abstract class AbstractKotlinInlineDeclarationProcessor<TDeclaration : KtNamedDeclaration>(
    protected val declaration: TDeclaration,
    private val reference: KtSimpleNameReference?,
    private val inlineThisOnly: Boolean,
    private val deleteAfter: Boolean,
    protected val editor: Editor?,
) : BaseRefactoringProcessor(declaration.project) {
    private lateinit var inliners: Map<Language, InlineHandler.Inliner>

    protected val kind = when (declaration) {
        is KtNamedFunction -> KotlinBundle.message("text.function")
        is KtProperty -> if (declaration.isLocal)
            KotlinBundle.message("text.local.variable")
        else
            KotlinBundle.message("text.local.property")
        else -> KotlinBundle.message("text.declaration")
    }

    private val commandName = KotlinBundle.message(
        "text.inlining.0.1",
        kind,
        DescriptiveNameUtil.getDescriptiveName(declaration)
    )

    abstract fun createReplacementStrategy(): UsageReplacementStrategy?

    open fun postAction() = Unit
    open fun postDeleteAction() = Unit

    final override fun findUsages(): Array<UsageInfo> {
        if (inlineThisOnly && reference != null) return arrayOf(UsageInfo(reference))
        val usages = hashSetOf<UsageInfo>()
        for (usage in ReferencesSearchScopeHelper.search(declaration, myRefactoringScope)) {
            usages += UsageInfo(usage)
        }

        if (deleteAfter) {
            declaration.forEachOverridingElement(scope = myRefactoringScope) { _, overridingMember ->
                val superMethods = findSuperMethodsNoWrapping(overridingMember)
                if (superMethods.singleOrNull()?.unwrapped == declaration) {
                    usages += OverrideUsageInfo(overridingMember)
                    return@forEachOverridingElement true
                }

                true
            }
        }

        return usages.toArray(UsageInfo.EMPTY_ARRAY)
    }

    open fun additionalPreprocessUsages(usages: Array<out UsageInfo>, conflicts: MultiMap<PsiElement, String>): Boolean = true

    final override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usagesInfo = refUsages.get()
        val conflicts = MultiMap<PsiElement, String>()
        if (!additionalPreprocessUsages(usagesInfo, conflicts)) return false

        if (deleteAfter) {
            for (superDeclaration in findSuperMethodsNoWrapping(declaration)) {
                val fqName = superDeclaration.getKotlinFqName()?.asString() ?: KotlinBundle.message("fix.change.signature.error")
                val message = KotlinBundle.message("text.inlined.0.overrides.0.1", kind, fqName)
                conflicts.putValue(superDeclaration, message)
            }
        }

        inliners = GenericInlineHandler.initInliners(
            declaration,
            usagesInfo,
            InlineHandler.Settings { inlineThisOnly },
            conflicts,
            KotlinLanguage.INSTANCE
        )

        return showConflicts(conflicts, usagesInfo)
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val replacementStrategy = createReplacementStrategy() ?: return

        val (kotlinReferenceUsages, nonKotlinReferenceUsages) = usages.partition { it !is OverrideUsageInfo && it.element is KtReferenceExpression }
        for (usage in nonKotlinReferenceUsages) {
            val element = usage.element ?: continue
            when {
                usage is OverrideUsageInfo -> for (processor in OverrideMethodsProcessor.EP_NAME.extensionList) {
                    if (processor.removeOverrideAttribute(element)) break
                }

                element.language == KotlinLanguage.INSTANCE -> LOG.error("Found unexpected Kotlin usage $element")
                else -> GenericInlineHandler.inlineReference(usage, declaration, inliners)
            }
        }

        replacementStrategy.replaceUsages(
            kotlinReferenceUsages.mapNotNull { it.element as? KtReferenceExpression },
            myProject,
            commandName
        ) {
            if (deleteAfter) {
                declaration.deleteWithCompanion()
                postDeleteAction()
            }

            postAction()
        }
    }

    override fun getCommandName(): String = commandName

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor = object : UsageViewDescriptor {
        override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) = RefactoringBundle.message(
            "comments.elements.header",
            UsageViewBundle.getOccurencesString(usagesCount, filesCount),
        )

        override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) = RefactoringBundle.message(
            "invocations.to.be.inlined",
            UsageViewBundle.getReferencesString(usagesCount, filesCount),
        )

        override fun getElements() = arrayOf<KtNamedDeclaration>(declaration)

        override fun getProcessedElementsHeader() = KotlinBundle.message("text.0.to.inline", kind.capitalize())
    }
}

private class OverrideUsageInfo(element: PsiElement) : UsageInfo(element)
