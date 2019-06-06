/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionsFactory
import org.jetbrains.kotlin.idea.quickfix.QuickFixActionBase
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.utils.mapToIndex
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


class InspectionLikeProcessingGroup(
    override val description: String,
    val inspectionLikeProcessings: List<InspectionLikeProcessing>
) : ProcessingGroup {

    constructor(description: String, vararg inspectionLikeProcessings: InspectionLikeProcessing) :
            this(description, inspectionLikeProcessings.toList())

    private val processingsToPriorityMap = inspectionLikeProcessings.mapToIndex()
    fun priority(processing: InspectionLikeProcessing): Int = processingsToPriorityMap.getValue(processing)

    override suspend fun runProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        do {
            var modificationStamp: Long? = file.modificationStamp
            val elementToActions = runReadAction {
                collectAvailableActions(file, converterContext, rangeMarker)
            }
            withContext(EDT) {
                CommandProcessor.getInstance().runUndoTransparentAction {
                    for ((element, action, _, writeActionNeeded) in elementToActions) {
                        if (element.isValid) {
                            if (writeActionNeeded) {
                                runWriteAction {
                                    runAction(action, element)
                                }
                            } else {
                                runAction(action, element)
                            }
                        } else {
                            modificationStamp = null
                        }
                    }
                }
            }
        } while (modificationStamp != file.modificationStamp && elementToActions.isNotEmpty())
    }


    private enum class RangeFilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    private inline fun runAction(action: () -> Unit, element: PsiElement) {
        val file = element.containingFile //element can be deleted after action performed
        action()
        runWriteAction { file.commitAndUnblockDocument() }
    }

    private data class ActionData(val element: PsiElement, val action: () -> Unit, val priority: Int, val writeActionNeeded: Boolean)

    private fun collectAvailableActions(
        file: KtFile,
        context: NewJ2kConverterContext,
        rangeMarker: RangeMarker?
    ): List<ActionData> {
        val diagnostics = analyzeFileRange(file, rangeMarker)

        val availableActions = ArrayList<ActionData>()

        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val rangeResult = rangeFilter(element, rangeMarker)
                if (rangeResult == RangeFilterResult.SKIP) return

                super.visitElement(element)

                if (rangeResult == RangeFilterResult.PROCESS) {
                    inspectionLikeProcessings.forEach { processing ->
                        val action = processing.createAction(element, diagnostics, context.converter.settings)
                        if (action != null) {
                            availableActions.add(
                                ActionData(
                                    element, action,
                                    priority(processing),
                                    processing.writeActionNeeded
                                )
                            )
                        }
                    }
                }
            }
        })
        availableActions.sortBy { it.priority }
        return availableActions
    }

    private fun analyzeFileRange(file: KtFile, rangeMarker: RangeMarker?): Diagnostics {
        val elements = if (rangeMarker == null)
            listOf(file)
        else
            file.elementsInRange(rangeMarker.range!!).filterIsInstance<KtElement>()

        return if (elements.isNotEmpty())
            file.getResolutionFacade().analyzeWithAllCompilerChecks(elements).bindingContext.diagnostics
        else
            Diagnostics.EMPTY
    }

    private fun rangeFilter(element: PsiElement, rangeMarker: RangeMarker?): RangeFilterResult {
        if (rangeMarker == null) return RangeFilterResult.PROCESS
        if (!rangeMarker.isValid) return RangeFilterResult.SKIP
        val range = TextRange(rangeMarker.startOffset, rangeMarker.endOffset)
        val elementRange = element.textRange
        return when {
            range.contains(elementRange) -> RangeFilterResult.PROCESS
            range.intersects(elementRange) -> RangeFilterResult.GO_INSIDE
            else -> RangeFilterResult.SKIP
        }
    }
}

interface InspectionLikeProcessing {
    fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)?

    val writeActionNeeded: Boolean
}

abstract class ApplicabilityBasedInspectionLikeProcessing<E : PsiElement>(private val classTag: KClass<E>) : InspectionLikeProcessing {
    protected abstract fun isApplicableTo(element: E, settings: ConverterSettings?): Boolean
    protected abstract fun apply(element: E)

    final override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
        if (!element::class.isSubclassOf(classTag)) return null
        @Suppress("UNCHECKED_CAST")
        if (!isApplicableTo(element as E, settings)) return null
        return {
            if (element.isValid && isApplicableTo(element, settings)) {
                apply(element)
            }
        }
    }

    final override val writeActionNeeded: Boolean = true
}


inline fun <reified TElement : PsiElement, TIntention : SelfTargetingRangeIntention<TElement>> intentionBasedProcessing(
    intention: TIntention,
    noinline additionalChecker: (TElement) -> Boolean = { true }
) = object : InspectionLikeProcessing {
    // Intention can either need or not need write apply
    override val writeActionNeeded = intention.startInWriteAction()

    override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
        if (!TElement::class.java.isInstance(element)) return null
        val tElement = element as TElement
        if (intention.applicabilityRange(tElement) == null) return null
        if (!additionalChecker(tElement)) return null
        return {
            if (intention.applicabilityRange(tElement) != null) { // isApplicableTo availability of the intention again because something could change
                intention.applyTo(element, null)
            }
        }
    }
}


fun <TInspection : AbstractKotlinInspection> generalInspectionBasedProcessing(
    inspection: TInspection,
    acceptInformationLevel: Boolean = false
) = (object : InspectionLikeProcessing {
    override val writeActionNeeded = false

    fun <D : CommonProblemDescriptor> QuickFix<D>.applyFixSmart(element: PsiElement, descriptor: D) {
        if (!element.isValid) return
        if (descriptor is ProblemDescriptor) {
            if (this is IntentionWrapper) {
                @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
                fun applySelfTargetingIntention(action: SelfTargetingIntention<PsiElement>) {
                    val target = action.getTarget(descriptor.psiElement.startOffset, descriptor.psiElement.containingFile) ?: return
                    if (!action.isApplicableTo(target, descriptor.psiElement.startOffset)) return
                    action.applyTo(target, null)
                }

                @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
                fun applyQuickFixActionBase(action: QuickFixActionBase<PsiElement>) {
                    if (!action.isAvailable(element.project, null, descriptor.psiElement.containingFile)) return
                    action.invoke(element.project, null, descriptor.psiElement.containingFile)
                }


                @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
                fun applyIntention() {
                    @Suppress("UNCHECKED_CAST")
                    when (val action = this.action) {
                        is SelfTargetingIntention<*> -> applySelfTargetingIntention(action as SelfTargetingIntention<PsiElement>)
                        is QuickFixActionBase<*> -> applyQuickFixActionBase(action)
                    }
                }


                if (this.startInWriteAction()) {
                    ApplicationManager.getApplication().runWriteAction(::applyIntention)
                } else {
                    applyIntention()
                }

            }
        }

        ApplicationManager.getApplication().runWriteAction {
            this.applyFix(element.project, descriptor)
        }
    }

    override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
        val holder = ProblemsHolder(InspectionManager.getInstance(element.project), element.containingFile, false)
        val visitor = inspection.buildVisitor(
            holder,
            false,
            LocalInspectionToolSession(element.containingFile, 0, element.containingFile.endOffset)
        )
        element.accept(visitor)
        if (!holder.hasResults()) return null
        return {
            holder.results.clear()
            element.accept(visitor)
            if (holder.hasResults()) {
                holder.results
                    .filter { acceptInformationLevel || it.highlightType != ProblemHighlightType.INFORMATION }
                    .forEach { it.fixes?.firstOrNull()?.applyFixSmart(element, it) }
            }
        }
    }
})


inline fun <reified TElement : PsiElement, TInspection : AbstractApplicabilityBasedInspection<TElement>>
        inspectionBasedProcessing(inspection: TInspection, acceptInformationLevel: Boolean = false) =
    object : InspectionLikeProcessing {
        // Inspection can either need or not need write apply
        override val writeActionNeeded = inspection.startFixInWriteAction

        fun isApplicable(element: TElement): Boolean {
            if (!inspection.isApplicable(element)) return false
            return acceptInformationLevel || inspection.inspectionHighlightType(element) != ProblemHighlightType.INFORMATION
        }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
            if (!TElement::class.java.isInstance(element)) return null
            val tElement = element as TElement
            if (!isApplicable(tElement)) return null
            return {
                if (isApplicable(tElement)) { // isApplicableTo availability of the inspection again because something could change
                    inspection.applyTo(tElement)
                }
            }
        }
    }

fun diagnosticBasedProcessingWithFixFactory(
    fixFactory: KotlinIntentionActionsFactory,
    vararg diagnosticFactory: DiagnosticFactory<*>
): InspectionLikeProcessing =
    diagnosticBasedProcessing(*diagnosticFactory) { element: PsiElement, diagnostic: Diagnostic ->
        fixFactory.createActions(diagnostic).singleOrNull()
            ?.invoke(element.project, null, element.containingFile)
    }


inline fun <reified TElement : PsiElement> diagnosticBasedProcessing(
    vararg diagnosticFactory: DiagnosticFactory<*>,
    crossinline fix: (TElement, Diagnostic) -> Unit
) = object : InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
        if (!TElement::class.java.isInstance(element)) return null
        val diagnostic = diagnostics.forElement(element).firstOrNull { it.factory in diagnosticFactory } ?: return null
        return {
            fix(element as TElement, diagnostic)
        }
    }
}

inline fun <reified TElement : PsiElement> diagnosticBasedProcessingFactory(
    vararg diagnosticFactory: DiagnosticFactory<*>,
    crossinline fixFactory: (TElement, Diagnostic) -> (() -> Unit)?
) = object : InspectionLikeProcessing {
    override val writeActionNeeded = true

    override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
        if (!TElement::class.java.isInstance(element)) return null
        val diagnostic = diagnostics.forElement(element).firstOrNull { it.factory in diagnosticFactory } ?: return null
        return fixFactory(element as TElement, diagnostic)
    }
}
