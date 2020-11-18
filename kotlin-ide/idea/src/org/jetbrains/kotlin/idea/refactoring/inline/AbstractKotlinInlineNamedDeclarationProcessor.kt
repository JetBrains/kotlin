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
import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.OverrideMethodsProcessor
import com.intellij.refactoring.inline.GenericInlineHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
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
import org.jetbrains.kotlin.idea.search.declarationsSearch.findSuperMethodsNoWrapping
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.psi.*

abstract class AbstractKotlinInlineNamedDeclarationProcessor<TDeclaration : KtNamedDeclaration>(
    declaration: TDeclaration,
    private val reference: PsiReference?,
    private val inlineThisOnly: Boolean,
    private val deleteAfter: Boolean,
    editor: Editor?,
    project: Project,
) : AbstractKotlinDeclarationInlineProcessor<TDeclaration>(declaration, editor, project) {
    private lateinit var inliners: Map<Language, InlineHandler.Inliner>

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

    open fun additionalPreprocessUsages(usages: Array<out UsageInfo>, conflicts: MultiMap<PsiElement, String>) = Unit

    final override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        if (deleteAfter && isWritable) {
            if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, declaration)) return false
        }

        val usagesInfo = refUsages.get()
        val conflicts = MultiMap<PsiElement, String>()
        additionalPreprocessUsages(usagesInfo, conflicts)
        for (usage in usagesInfo) {
            val element = usage.element ?: continue
            val callableConflict = findCallableConflictForUsage(element) ?: continue
            conflicts.putValue(element, callableConflict)
        }

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

    private fun postActions() {
        if (deleteAfter && isWritable) {
            declaration.deleteWithCompanion()
            postDeleteAction()
        }

        postAction()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        if (usages.isEmpty()) {
            if (!deleteAfter) {
                val message = KotlinBundle.message("0.1.is.never.used", kind.capitalize(), declaration.name.toString())
                CommonRefactoringUtil.showErrorHint(myProject, editor, message, commandName, null)
            } else {
                postActions()
            }

            return
        }

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
            usages = kotlinReferenceUsages.mapNotNull { it.element as? KtReferenceExpression }
        )

        postActions()
    }

    private val isWritable: Boolean
        get() = declaration.isWritable

    override fun getElementsToWrite(descriptor: UsageViewDescriptor): Collection<PsiElement?> = when {
        inlineThisOnly -> listOfNotNull(reference?.element)
        isWritable -> listOfNotNull(reference?.element, declaration)
        else -> emptyList()
    }

    companion object {
        private val LOG = Logger.getInstance(AbstractKotlinInlineNamedDeclarationProcessor::class.java)
    }
}

private class OverrideUsageInfo(element: PsiElement) : UsageInfo(element)
