/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.api.applicator.*
import org.jetbrains.kotlin.idea.frontend.api.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.analyzeWithReadAction
import org.jetbrains.kotlin.idea.frontend.api.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitorVoid
import kotlin.reflect.KClass

abstract class AbstractHLInspection<PSI : KtElement, INPUT : HLApplicatorInput>(
    val elementType: KClass<PSI>
) : AbstractKotlinInspection() {
    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)

                if (!elementType.isInstance(element) || element.textLength == 0) return
                @Suppress("UNCHECKED_CAST")
                visitTargetElement(element as PSI, holder, isOnTheFly)
            }
        }

    private fun visitTargetElement(element: PSI, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (!applicator.isApplicableByPsi(element)) return
        val targets = applicabilityRange.getApplicabilityRanges(element)
        if (targets.isEmpty()) return

        val input = getInput(element) ?: return
        require(input.isValidFor(element)) { "Input should be valid after creation" }

        registerProblems(holder, element, targets, isOnTheFly, input)
    }


    private fun registerProblems(
        holder: ProblemsHolder,
        element: PSI,
        ranges: List<TextRange>,
        isOnTheFly: Boolean,
        input: INPUT
    ) {
        val highlightType = presentation.getHighlightType(element)
        if (!isOnTheFly && highlightType == ProblemHighlightType.INFORMATION) return

        val description = applicator.getActionName(element, input)
        val fix = applicator.asLocalQuickFix(input, actionName = applicator.getActionName(element, input))

        ranges.forEach { range ->
            registerProblem(holder, element, range, description, highlightType, isOnTheFly, fix)
        }
    }

    private fun registerProblem(
        holder: ProblemsHolder,
        element: PSI,
        range: TextRange,
        description: String,
        highlightType: ProblemHighlightType,
        isOnTheFly: Boolean,
        fix: LocalQuickFix
    ) {
        with(holder) {
            val problemDescriptor = manager.createProblemDescriptor(element, range, description, highlightType, isOnTheFly, fix)
            registerProblem(problemDescriptor)
        }
    }

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    private fun getInput(element: PSI): INPUT? = hackyAllowRunningOnEdt {
        analyzeWithReadAction(element) {
            with(inputProvider) { provideInput(element) }
        }
    }


    abstract val presentation: HLPresentation<PSI>
    abstract val applicabilityRange: HLApplicabilityRange<PSI>
    abstract val inputProvider: HLApplicatorInputProvider<PSI, INPUT>
    abstract val applicator: HLApplicator<PSI, INPUT>
}

private fun <PSI : PsiElement, INPUT : HLApplicatorInput> HLApplicator<PSI, INPUT>.asLocalQuickFix(
    input: INPUT,
    actionName: String,
): LocalQuickFix = object : LocalQuickFix {
    override fun startInWriteAction() = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        @Suppress("UNCHECKED_CAST")
        val element = descriptor.psiElement as PSI

        if (isApplicableByPsi(element) && input.isValidFor(element)) {
            applyTo(element, input, project, editor = null)
        }
    }

    override fun getFamilyName() = this@asLocalQuickFix.getFamilyName()
    override fun getName() = actionName
}