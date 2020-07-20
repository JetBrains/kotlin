/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.RefactoringHelper
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.inspections.KotlinUnusedImportInspection
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

// Based on com.intellij.refactoring.OptimizeImportsRefactoringHelper
class KotlinOptimizeImportsRefactoringHelper : RefactoringHelper<Set<KtFile>> {
    internal open class CollectUnusedImportsTask(
        project: Project,
        private val dumbService: DumbService,
        private val unusedImports: MutableSet<SmartPsiElementPointer<KtImportDirective>>,
        private val operationData: Set<KtFile>
    ) : Task.Backgroundable(project, COLLECT_UNUSED_IMPORTS_TITLE, false) {

        override fun run(indicator: ProgressIndicator) {
            val myTotalCount = operationData.size
            for ((counter, file) in operationData.withIndex()) {
                if (!file.isValid) return
                val virtualFile = file.virtualFile ?: return

                with(indicator) {
                    text2 = virtualFile.presentableUrl
                    fraction = counter.toDouble() / myTotalCount
                }

                dumbService.runReadActionInSmartMode {
                    KotlinUnusedImportInspection.analyzeImports(file)?.unusedImports?.mapTo(unusedImports) { it.createSmartPointer() }
                }
            }
        }
    }

    internal class OptimizeImportsTask(
        project: Project,
        private val pointers: Set<SmartPsiElementPointer<KtImportDirective>>
    ) : Task.Modal(project, REMOVING_REDUNDANT_IMPORTS_TITLE, false) {

        override fun run(indicator: ProgressIndicator) {
            val myTotal: Int = pointers.size
            for ((counter, pointer) in pointers.withIndex()) {
                indicator.fraction = counter.toDouble() / myTotal

                runReadAction { pointer.element?.takeIf { it.isValid }?.containingKtFile?.virtualFile }?.let { virtualFile ->
                    val presentableUrl = virtualFile.presentableUrl
                    indicator.text2 = presentableUrl
                    project.executeWriteCommand(KotlinBundle.message("delete.0", presentableUrl)) {
                        try {
                            pointer.element?.delete()
                        } catch (e: IncorrectOperationException) {
                            LOG.error(e)
                        }
                    }
                }
            }
        }

        companion object {
            private val LOG = Logger.getInstance("#" + OptimizeImportsTask::class.java.name)
        }
    }

    companion object {
        private val COLLECT_UNUSED_IMPORTS_TITLE = KotlinBundle.message("optimize.imports.collect.unused.imports")
        private val REMOVING_REDUNDANT_IMPORTS_TITLE = KotlinBundle.message("optimize.imports.task.removing.redundant.imports")
    }

    override fun prepareOperation(usages: Array<UsageInfo>): Set<KtFile> = usages.mapNotNullTo(LinkedHashSet()) {
        if (!it.isNonCodeUsage) it.file as? KtFile else null
    }

    override fun performOperation(project: Project, operationData: Set<KtFile>) {
        if (operationData.isEmpty()) return

        CodeStyleManager.getInstance(project).performActionWithFormatterDisabled {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
        }

        val unusedImports = mutableSetOf<SmartPsiElementPointer<KtImportDirective>>()

        val progressManager = ProgressManager.getInstance()

        val dumbService = DumbService.getInstance(project)

        val collectTask = object : CollectUnusedImportsTask(project, dumbService, unusedImports, operationData) {
            override fun onSuccess() {
                val progressTask = OptimizeImportsTask(project, unusedImports)
                progressManager.run(progressTask)
            }
        }

        progressManager.run(collectTask)
    }
}
