/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightRangeExtension
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.fir.FirResolution
import org.jetbrains.kotlin.idea.fir.firResolveState
import org.jetbrains.kotlin.idea.fir.getOrBuildFirWithDiagnostics
import org.jetbrains.kotlin.idea.quickfix.QuickFixes
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.lang.reflect.*
import java.util.*

open class KotlinPsiChecker : Annotator, HighlightRangeExtension {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile as? KtFile ?: return

        if (!KotlinHighlightingUtil.shouldHighlight(file)) return

        //todo move all fir stuff to fir plugin
        if (FirResolution.enabled) {
            annotateElementUsingFrontendIR(element, file, holder)
        } else {
            annotateElement(element, file, holder)
        }
    }

    private fun annotateElement(
        element: PsiElement,
        containingFile: KtFile,
        holder: AnnotationHolder
    ) {
        val analysisResult = containingFile.analyzeWithAllCompilerChecks()
        if (analysisResult.isError()) {
            throw ProcessCanceledException(analysisResult.error)
        }

        val bindingContext = analysisResult.bindingContext

        getAfterAnalysisVisitor(holder, bindingContext).forEach { visitor -> element.accept(visitor) }

        annotateElement(element, holder, bindingContext.diagnostics)
    }

    private fun annotateElementUsingFrontendIR(
        element: PsiElement,
        containingFile: KtFile,
        holder: AnnotationHolder
    ) {
        if (element !is KtElement) return
        val state = containingFile.firResolveState()
        containingFile.getOrBuildFirWithDiagnostics(state)

        val diagnostics = state.getDiagnostics(element)
        if (diagnostics.isEmpty()) return

        if (KotlinHighlightingUtil.shouldHighlightErrors(element)) {
            ElementAnnotator(element, holder) { param ->
                shouldSuppressUnusedParameter(param)
            }.registerDiagnosticsAnnotations(diagnostics)
        }
    }

    override fun isForceHighlightParents(file: PsiFile): Boolean {
        return file is KtFile
    }

    protected open fun shouldSuppressUnusedParameter(parameter: KtParameter): Boolean = false

    fun annotateElement(element: PsiElement, holder: AnnotationHolder, diagnostics: Diagnostics) {
        val diagnosticsForElement = diagnostics.forElement(element).toSet()

        if (element is KtNameReferenceExpression) {
            val unresolved = diagnostics.any { it.factory == Errors.UNRESOLVED_REFERENCE }
            element.putUserData(UNRESOLVED_KEY, if (unresolved) Unit else null)
        }

        if (diagnosticsForElement.isEmpty()) return

        if (KotlinHighlightingUtil.shouldHighlightErrors(element)) {
            ElementAnnotator(element, holder) { param ->
                shouldSuppressUnusedParameter(param)
            }.registerDiagnosticsAnnotations(diagnosticsForElement)
        }
    }

    companion object {
        fun getAfterAnalysisVisitor(holder: AnnotationHolder, bindingContext: BindingContext) = arrayOf(
            PropertiesHighlightingVisitor(holder, bindingContext),
            FunctionsHighlightingVisitor(holder, bindingContext),
            VariablesHighlightingVisitor(holder, bindingContext),
            TypeKindHighlightingVisitor(holder, bindingContext)
        )

        fun createQuickFixes(diagnostic: Diagnostic): Collection<IntentionAction> =
            createQuickFixes(listOfNotNull(diagnostic))[diagnostic]

        private val UNRESOLVED_KEY = Key<Unit>("KotlinPsiChecker.UNRESOLVED_KEY")

        fun wasUnresolved(element: KtNameReferenceExpression) = element.getUserData(UNRESOLVED_KEY) != null
    }
}

private fun createQuickFixes(similarDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> {
    val first = similarDiagnostics.minBy { it.toString() }
    val factory = similarDiagnostics.first().getRealDiagnosticFactory()

    val actions = MultiMap<Diagnostic, IntentionAction>()

    val intentionActionsFactories = QuickFixes.getInstance().getActionFactories(factory)
    for (intentionActionsFactory in intentionActionsFactories) {
        val allProblemsActions = intentionActionsFactory.createActionsForAllProblems(similarDiagnostics)
        if (allProblemsActions.isNotEmpty()) {
            actions.putValues(first, allProblemsActions)
        } else {
            for (diagnostic in similarDiagnostics) {
                actions.putValues(diagnostic, intentionActionsFactory.createActions(diagnostic))
            }
        }
    }

    for (diagnostic in similarDiagnostics) {
        actions.putValues(diagnostic, QuickFixes.getInstance().getActions(diagnostic.factory))
    }

    actions.values().forEach { NoDeclarationDescriptorsChecker.check(it::class.java) }

    return actions
}

private fun Diagnostic.getRealDiagnosticFactory(): DiagnosticFactory<*> =
    when (factory) {
        Errors.PLUGIN_ERROR -> Errors.PLUGIN_ERROR.cast(this).a.factory
        Errors.PLUGIN_WARNING -> Errors.PLUGIN_WARNING.cast(this).a.factory
        Errors.PLUGIN_INFO -> Errors.PLUGIN_INFO.cast(this).a.factory
        else -> factory
    }

private object NoDeclarationDescriptorsChecker {
    private val LOG = Logger.getInstance(NoDeclarationDescriptorsChecker::class.java)

    private val checkedQuickFixClasses = Collections.synchronizedSet(HashSet<Class<*>>())

    fun check(quickFixClass: Class<*>) {
        if (!checkedQuickFixClasses.add(quickFixClass)) return

        for (field in quickFixClass.declaredFields) {
            checkType(field.genericType, field)
        }

        quickFixClass.superclass?.let { check(it) }
    }

    private fun checkType(type: Type, field: Field) {
        when (type) {
            is Class<*> -> {
                if (DeclarationDescriptor::class.java.isAssignableFrom(type) || KotlinType::class.java.isAssignableFrom(type)) {
                    LOG.error(
                        "QuickFix class ${field.declaringClass.name} contains field ${field.name} that holds ${type.simpleName}. "
                                + "This leads to holding too much memory through this quick-fix instance. "
                                + "Possible solution can be wrapping it using KotlinIntentionActionFactoryWithDelegate."
                    )
                }

                if (IntentionAction::class.java.isAssignableFrom(type)) {
                    check(type)
                }

            }

            is GenericArrayType -> checkType(type.genericComponentType, field)

            is ParameterizedType -> {
                if (Collection::class.java.isAssignableFrom(type.rawType as Class<*>)) {
                    type.actualTypeArguments.forEach { checkType(it, field) }
                }
            }

            is WildcardType -> type.upperBounds.forEach { checkType(it, field) }
        }
    }
}

private class ElementAnnotator(
    private val element: PsiElement,
    private val holder: AnnotationHolder,
    private val shouldSuppressUnusedParameter: (KtParameter) -> Boolean
) {
    fun registerDiagnosticsAnnotations(diagnostics: Collection<Diagnostic>) {
        diagnostics.groupBy { it.factory }.forEach { group -> registerDiagnosticAnnotations(group.value) }
    }

    private fun registerDiagnosticAnnotations(diagnostics: List<Diagnostic>) {
        assert(diagnostics.isNotEmpty())

        val validDiagnostics = diagnostics.filter { it.isValid }
        if (validDiagnostics.isEmpty()) return

        val diagnostic = diagnostics.first()
        val factory = diagnostic.factory

        // hack till the root cause #KT-21246 is fixed
        if (isIrCompileClassDiagnosticForModulesWithEnabledIR(diagnostic)) return

        assert(diagnostics.all { it.psiElement == element && it.factory == factory })

        val ranges = diagnostic.textRanges

        val presentationInfo: AnnotationPresentationInfo = when (factory.severity) {
            Severity.ERROR -> {
                when (factory) {
                    in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS -> {
                        val referenceExpression = element as KtReferenceExpression
                        val reference = referenceExpression.mainReference
                        if (reference is MultiRangeReference) {
                            AnnotationPresentationInfo(
                                ranges = reference.ranges.map { it.shiftRight(referenceExpression.textOffset) },
                                highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                            )
                        } else {
                            AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        }
                    }

                    Errors.ILLEGAL_ESCAPE -> AnnotationPresentationInfo(
                        ranges, textAttributes = KotlinHighlightingColors.INVALID_STRING_ESCAPE
                    )

                    Errors.REDECLARATION -> AnnotationPresentationInfo(
                        ranges = listOf(diagnostic.textRanges.first()), nonDefaultMessage = ""
                    )

                    else -> {
                        AnnotationPresentationInfo(
                            ranges,
                            highlightType = if (factory == Errors.INVISIBLE_REFERENCE)
                                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                            else
                                null
                        )
                    }
                }
            }
            Severity.WARNING -> {
                if (factory == Errors.UNUSED_PARAMETER && shouldSuppressUnusedParameter(element as KtParameter)) {
                    return
                }

                AnnotationPresentationInfo(
                    ranges,
                    textAttributes = when (factory) {
                        Errors.DEPRECATION -> CodeInsightColors.DEPRECATED_ATTRIBUTES
                        Errors.UNUSED_ANONYMOUS_PARAMETER -> CodeInsightColors.WEAK_WARNING_ATTRIBUTES
                        else -> null
                    },
                    highlightType = when (factory) {
                        in Errors.UNUSED_ELEMENT_DIAGNOSTICS -> ProblemHighlightType.LIKE_UNUSED_SYMBOL
                        Errors.UNUSED_ANONYMOUS_PARAMETER -> ProblemHighlightType.WEAK_WARNING
                        else -> null
                    }
                )
            }
            Severity.INFO -> AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.INFORMATION)
        }

        setUpAnnotations(diagnostics, presentationInfo)
    }

    private fun setUpAnnotations(diagnostics: List<Diagnostic>, data: AnnotationPresentationInfo) {
        val fixesMap = try {
            createQuickFixes(diagnostics)
        } catch (e: Exception) {
            if (e is ControlFlowException) {
                throw e
            }
            LOG.error(e)
            MultiMap<Diagnostic, IntentionAction>()
        }

        data.processDiagnostics(holder, diagnostics, fixesMap)
    }

    private fun isIrCompileClassDiagnosticForModulesWithEnabledIR(diagnostic: Diagnostic): Boolean {
        if (diagnostic.factory != Errors.IR_COMPILED_CLASS) return false
        val module = element.module ?: return false
        val moduleFacetSettings = KotlinFacetSettingsProvider.getInstance(element.project)?.getSettings(module) ?: return false
        return moduleFacetSettings.isCompilerSettingPresent(K2JVMCompilerArguments::useIR)
                || moduleFacetSettings.isCompilerSettingPresent(K2JVMCompilerArguments::allowJvmIrDependencies)
    }

    companion object {
        val LOG = Logger.getInstance(ElementAnnotator::class.java)
    }
}

