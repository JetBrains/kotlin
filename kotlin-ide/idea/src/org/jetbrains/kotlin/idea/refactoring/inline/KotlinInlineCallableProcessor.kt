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

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.Language
import com.intellij.lang.findUsages.DescriptiveNameUtil
import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.inline.GenericInlineHandler
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.replaceUsages
import org.jetbrains.kotlin.idea.findUsages.ReferencesSearchScopeHelper
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.pullUp.deleteWithCompanion
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.declarationsSearch.findSuperMethodsNoWrapping
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

private val LOG = Logger.getInstance(KotlinInlineCallableProcessor::class.java)

class KotlinInlineCallableProcessor(
    project: Project,
    private val replacementStrategy: UsageReplacementStrategy,
    private val declaration: KtCallableDeclaration,
    private val reference: KtSimpleNameReference?,
    private val inlineThisOnly: Boolean,
    private val deleteAfter: Boolean,
    private val statementToDelete: KtBinaryExpression? = null,
    private val postAction: (KtCallableDeclaration) -> Unit = {}
) : BaseRefactoringProcessor(project) {
    private lateinit var inliners: Map<Language, InlineHandler.Inliner>

    private val kind = when (declaration) {
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

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usagesInfo = refUsages.get()
        val conflicts = MultiMap<PsiElement, String>()
        if (!inlineThisOnly) {
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

    override fun findUsages(): Array<UsageInfo> {
        if (inlineThisOnly && reference != null) return arrayOf(UsageInfo(reference))
        val usages = hashSetOf<UsageInfo>()
        for (usage in ReferencesSearchScopeHelper.search(declaration, myRefactoringScope)) {
            usages += UsageInfo(usage)
        }

        declaration.forEachOverridingElement(scope = myRefactoringScope, searchDeeply = false) { _, overridingMember ->
            val hasOverrideModifier = when (overridingMember) {
                is PsiMethod -> AnnotationUtil.isAnnotated(overridingMember, Override::class.java.name, 0)
                is KtModifierListOwner -> overridingMember.hasModifier(KtTokens.OVERRIDE_KEYWORD)
                else -> false
            }

            if (hasOverrideModifier) {
                usages += UsageInfo(overridingMember)
            }

            true
        }

        return usages.toArray(UsageInfo.EMPTY_ARRAY)
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val (kotlinUsages, nonKotlinUsages) = usages.partition { it.element is KtReferenceExpression }
        for (usage in nonKotlinUsages) {
            val element = usage.element ?: continue
            when {
                element is KtNamedFunction || element is KtProperty || element is PsiMethod -> element.removeOverrideModifier()
                element.language == KotlinLanguage.INSTANCE -> LOG.error("Found unexpected Kotlin usage $element")
                else -> GenericInlineHandler.inlineReference(usage, element, inliners)
            }
        }

        replacementStrategy.replaceUsages(
            kotlinUsages.mapNotNull { it.element as? KtReferenceExpression },
            declaration,
            myProject,
            commandName,
            postAction = {
                if (deleteAfter) {
                    declaration.deleteWithCompanion()
                    statementToDelete?.delete()
                }

                postAction(declaration)
            }
        )
    }

    override fun getCommandName(): String = commandName

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return object : UsageViewDescriptor {
            override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) =
                RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount))

            override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) =
                RefactoringBundle.message("invocations.to.be.inlined", UsageViewBundle.getReferencesString(usagesCount, filesCount))

            override fun getElements() = arrayOf(declaration)

            override fun getProcessedElementsHeader() = KotlinBundle.message("text.0.to.inline", kind.capitalize())
        }
    }
}