/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactoryWithPsiElement
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid


abstract class AbstractDiagnosticBasedMigrationInspection<T : KtElement>(
    val elementType: Class<T>,
) : AbstractKotlinInspection() {
    abstract val diagnosticFactory: DiagnosticFactoryWithPsiElement<T, *>
    open fun getCustomIntentionFactory(): ((Diagnostic) -> IntentionAction?)? = null

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is KtFile) return null
        val diagnostics by lazy {
            file.analyzeWithAllCompilerChecks().bindingContext.diagnostics
        }

        val actionsFactory = QuickFixes.getInstance().getActionFactories(diagnosticFactory).singleOrNull() ?: error("Must have one factory")
        val problemDescriptors = mutableListOf<ProblemDescriptor>()

        file.accept(
            object : KtTreeVisitorVoid() {
                override fun visitKtElement(element: KtElement) {
                    super.visitKtElement(element)

                    if (!elementType.isInstance(element) || element.textLength == 0) return
                    val diagnostic = diagnostics.forElement(element)
                        .filter { it.factory == diagnosticFactory }
                        .ifEmpty { return }
                        .singleOrNull()
                        ?: error("Must have one diagnostic")

                    val customIntentionFactory = getCustomIntentionFactory()
                    val intentionAction = if (customIntentionFactory != null)
                        customIntentionFactory(diagnostic) ?: return
                    else
                        actionsFactory.createActions(diagnostic).ifEmpty { return }.singleOrNull() ?: error("Must have one fix")

                    val text = descriptionMessage() ?: DefaultErrorMessages.render(diagnostic)
                    problemDescriptors.add(
                        manager.createProblemDescriptor(
                            element,
                            text,
                            false,
                            arrayOf(IntentionWrapper(intentionAction, file)),
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        ),
                    )
                }
            },
        )

        return problemDescriptors.toTypedArray()
    }

    protected open fun descriptionMessage(): String? = null
}