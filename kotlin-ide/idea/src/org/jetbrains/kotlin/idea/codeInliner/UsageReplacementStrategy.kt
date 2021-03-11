/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInliner

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.ui.GuiUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.intentions.ConvertReferenceToLambdaIntention
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

interface UsageReplacementStrategy {
    fun createReplacer(usage: KtReferenceExpression): (() -> KtElement?)?

    companion object {
        val KEY = Key<Unit>("UsageReplacementStrategy.replaceUsages")
    }
}

private val LOG = Logger.getInstance(UsageReplacementStrategy::class.java)

fun UsageReplacementStrategy.replaceUsagesInWholeProject(
    targetPsiElement: PsiElement,
    @NlsContexts.DialogTitle progressTitle: String,
    commandName: String
) {
    val project = targetPsiElement.project
    ProgressManager.getInstance().run(
        object : Task.Modal(project, progressTitle, true) {
            override fun run(indicator: ProgressIndicator) {
                val usages = runReadAction {
                    val searchScope = KotlinSourceFilterScope.projectSources(GlobalSearchScope.projectScope(project), project)
                    ReferencesSearch.search(targetPsiElement, searchScope)
                        .filterIsInstance<KtSimpleReference<KtReferenceExpression>>()
                        .map { ref -> ref.expression }
                }

                GuiUtils.invokeLaterIfNeeded(
                    {
                        project.executeWriteCommand(commandName) {
                            this@replaceUsagesInWholeProject.replaceUsages(usages)
                        }
                    },
                    ModalityState.NON_MODAL
                )
            }
        })
}

fun UsageReplacementStrategy.replaceUsages(usages: Collection<KtReferenceExpression>) {
    val usagesByFile = usages.groupBy { it.containingFile }

    for ((file, usagesInFile) in usagesByFile) {
        usagesInFile.forEach { it.putCopyableUserData(UsageReplacementStrategy.KEY, Unit) }

        // we should delete imports later to not affect other usages
        val importsToDelete = mutableListOf<KtImportDirective>()

        var usagesToProcess = usagesInFile
        while (usagesToProcess.isNotEmpty()) {
            if (processUsages(usagesToProcess, importsToDelete)) break

            // some usages may get invalidated we need to find them in the tree
            usagesToProcess = file.collectDescendantsOfType { it.getCopyableUserData(UsageReplacementStrategy.KEY) != null }
        }

        file.forEachDescendantOfType<KtSimpleNameExpression> { it.putCopyableUserData(UsageReplacementStrategy.KEY, null) }

        importsToDelete.forEach { it.delete() }
    }
}

/**
 * @return false if some usages were invalidated
 */
private fun UsageReplacementStrategy.processUsages(
    usages: List<KtReferenceExpression>,
    importsToDelete: MutableList<KtImportDirective>,
): Boolean {
    val sortedUsages = usages.sortedWith { element1, element2 ->
        if (element1.parent.textRange.intersects(element2.parent.textRange)) {
            compareValuesBy(element2, element1) { it.startOffset }
        } else {
            compareValuesBy(element1, element2) { it.startOffset }
        }
    }

    var invalidUsagesFound = false
    for (usage in sortedUsages) {
        try {
            if (!usage.isValid) {
                invalidUsagesFound = true
                continue
            }

            val specialUsage = unwrapSpecialUsageOrNull(usage)
            if (specialUsage != null) {
                createReplacer(specialUsage)?.invoke()
                continue
            }

            //TODO: keep the import if we don't know how to replace some of the usages
            val importDirective = usage.getStrictParentOfType<KtImportDirective>()
            if (importDirective != null) {
                if (!importDirective.isAllUnder && importDirective.targetDescriptors().size == 1) {
                    importsToDelete.add(importDirective)
                }
                continue
            }

            createReplacer(usage)?.invoke()?.parent?.parent?.parent?.let { block ->
                CodeStyleManager.getInstance(block.project).reformat(block, true)
            }
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            LOG.error(e)
        }
    }
    return !invalidUsagesFound
}

fun unwrapSpecialUsageOrNull(
    usage: KtReferenceExpression
): KtSimpleNameExpression? {
    if (usage !is KtSimpleNameExpression) return null

    when (val usageParent = usage.parent) {
        is KtCallableReferenceExpression -> {
            if (usageParent.callableReference != usage) return null
            val (name, descriptor) = usage.nameAndDescriptor
            return ConvertReferenceToLambdaIntention.applyTo(usageParent)?.let {
                findNewUsage(it, name, descriptor)
            }
        }

        is KtCallElement -> {
            for (valueArgument in usageParent.valueArguments.asReversed()) {
                val callableReferenceExpression = valueArgument.getArgumentExpression() as? KtCallableReferenceExpression ?: continue
                ConvertReferenceToLambdaIntention.applyTo(callableReferenceExpression)
            }

            val lambdaExpressions = usageParent.valueArguments.mapNotNull { it.getArgumentExpression() as? KtLambdaExpression }
            if (lambdaExpressions.isEmpty()) return null

            val (name, descriptor) = usage.nameAndDescriptor
            val grandParent = usageParent.parent
            for (lambdaExpression in lambdaExpressions) {
                val functionDescriptor = lambdaExpression.functionLiteral.resolveToDescriptorIfAny() as? FunctionDescriptor ?: continue
                if (functionDescriptor.valueParameters.isNotEmpty()) {
                    SpecifyExplicitLambdaSignatureIntention.applyTo(lambdaExpression)
                }
            }

            return grandParent.safeAs<KtElement>()?.let {
                findNewUsage(it, name, descriptor)
            }
        }

    }

    return null
}

private val KtSimpleNameExpression.nameAndDescriptor get() = getReferencedName() to resolveToCall()?.candidateDescriptor

private fun findNewUsage(
    element: KtElement,
    targetName: String?,
    targetDescriptor: DeclarationDescriptor?
): KtSimpleNameExpression? = element.findDescendantOfType {
    it.getReferencedName() == targetName && compareDescriptors(it.project, targetDescriptor, it.resolveToCall()?.candidateDescriptor)
}
