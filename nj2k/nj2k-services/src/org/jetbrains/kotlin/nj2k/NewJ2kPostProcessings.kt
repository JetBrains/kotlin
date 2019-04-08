/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.idea.core.implicitVisibility
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.formatter.commitAndUnblockDocument
import org.jetbrains.kotlin.idea.inspections.*
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.idea.inspections.conventionNameCalls.ReplaceGetOrSetInspection
import org.jetbrains.kotlin.idea.intentions.*
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.FoldIfToReturnAsymmetricallyIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.FoldIfToReturnIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isTrivialStatementBody
import org.jetbrains.kotlin.idea.quickfix.*
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.mapToIndex
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

interface NewJ2kPostProcessing {
    fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? =
        createAction(element, diagnostics)

    fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? =
        createAction(element, diagnostics, null)

    val writeActionNeeded: Boolean
}

interface Processing
data class SingleOneTimeProcessing(val processing: NewJ2kPostProcessing) : Processing
data class RepeatableProcessingGroup(val processings: List<NewJ2kPostProcessing>) : Processing {
    constructor(vararg processings: NewJ2kPostProcessing) : this(processings.toList())
}

data class OneTimeProcessingGroup(val processings: List<Processing>) : Processing {
    constructor(vararg processings: Processing) : this(processings.toList())
}

private abstract class CheckableProcessing<E : PsiElement>(private val classTag: KClass<E>) : NewJ2kPostProcessing {
    protected open fun check(element: E): Boolean =
        check(element, null)

    protected open fun check(element: E, settings: ConverterSettings?): Boolean =
        check(element)

    protected abstract fun action(element: E)

    override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
        if (!element::class.isSubclassOf(classTag)) return null
        @Suppress("UNCHECKED_CAST")
        if (!check(element as E, settings)) return null
        return {
            if (element.isValid && check(element, settings)) {
                action(element)
            }
        }
    }

    override val writeActionNeeded: Boolean = true
}

object NewJ2KPostProcessingRegistrar {

    private fun Processing.processings(): Sequence<NewJ2kPostProcessing> =
        when (this) {
            is SingleOneTimeProcessing -> sequenceOf(processing)
            is RepeatableProcessingGroup -> processings.asSequence()
            is OneTimeProcessingGroup -> processings.asSequence().flatMap { it.processings() }
            else -> sequenceOf()
        }


    private val processingsToPriorityMap = HashMap<NewJ2kPostProcessing, Int>()

    fun priority(processing: NewJ2kPostProcessing): Int = processingsToPriorityMap[processing]!!

    val mainProcessings = OneTimeProcessingGroup(
        SingleOneTimeProcessing(VarToVal()),
        SingleOneTimeProcessing(ConvertGettersAndSetters()),
        SingleOneTimeProcessing(MoveGetterAndSetterAnnotationsToProperty()),
        SingleOneTimeProcessing(registerGeneralInspectionBasedProcessing(RedundantGetterInspection())),
        SingleOneTimeProcessing(registerGeneralInspectionBasedProcessing(RedundantSetterInspection())),
        SingleOneTimeProcessing(ConvertDataClass()),
        RepeatableProcessingGroup(
            RemoveRedundantVisibilityModifierProcessing(),
            RemoveRedundantModalityModifierProcessing(),
            RemoveRedundantConstructorKeywordProcessing(),
            registerDiagnosticBasedProcessing(Errors.REDUNDANT_OPEN_IN_INTERFACE) { element: KtDeclaration, diagnostic ->
                element.removeModifier(KtTokens.OPEN_KEYWORD)
            },
            object : NewJ2kPostProcessing {
                override val writeActionNeeded: Boolean = true

                override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
                    if (element !is KtClass) return null

                    fun check(klass: KtClass): Boolean {
                        return klass.isValid
                                && klass.isInterface()
                                && klass.hasModifier(KtTokens.OPEN_KEYWORD)
                    }

                    if (!check(element)) return null
                    return {
                        if (check(element)) {
                            element.removeModifier(KtTokens.OPEN_KEYWORD)
                        }
                    }
                }
            },

            registerGeneralInspectionBasedProcessing(ExplicitThisInspection()),
            RemoveExplicitTypeArgumentsProcessing(),
            RemoveRedundantOverrideVisibilityProcessing(),
            registerInspectionBasedProcessing(MoveLambdaOutsideParenthesesInspection()),
            registerGeneralInspectionBasedProcessing(RedundantCompanionReferenceInspection()),
            FixObjectStringConcatenationProcessing(),
            ConvertToStringTemplateProcessing(),
            UsePropertyAccessSyntaxProcessing(),
            UninitializedVariableReferenceFromInitializerToThisReferenceProcessing(),
            UnresolvedVariableReferenceFromInitializerToThisReferenceProcessing(),
            RemoveRedundantSamAdaptersProcessing(),
            RemoveRedundantCastToNullableProcessing(),
            registerInspectionBasedProcessing(ReplacePutWithAssignmentInspection()),
            UseExpressionBodyProcessing(),
            registerInspectionBasedProcessing(UnnecessaryVariableInspection()),

            object : NewJ2kPostProcessing {
                override val writeActionNeeded: Boolean = true
                private val processing = registerGeneralInspectionBasedProcessing(RedundantExplicitTypeInspection())

                override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
                    if (settings?.specifyLocalVariableTypeByDefault == true) return null

                    return processing.createAction(element, diagnostics)
                }
            }
            ,
            registerGeneralInspectionBasedProcessing(RedundantUnitReturnTypeInspection()),
            JavaObjectEqualsToEqOperator(),
            RemoveExplicitPropertyType(),
            RemoveRedundantNullability(),

            registerGeneralInspectionBasedProcessing(CanBeValInspection(ignoreNotUsedVals = false)),

            registerIntentionBasedProcessing(FoldInitializerAndIfToElvisIntention()),
            registerGeneralInspectionBasedProcessing(RedundantSemicolonInspection()),
            registerIntentionBasedProcessing(RemoveEmptyClassBodyIntention()),
            registerIntentionBasedProcessing(RemoveRedundantCallsOfConversionMethodsIntention()),

            registerIntentionBasedProcessing(FoldIfToReturnIntention()) { it.then.isTrivialStatementBody() && it.`else`.isTrivialStatementBody() },
            registerIntentionBasedProcessing(FoldIfToReturnAsymmetricallyIntention()) {
                it.then.isTrivialStatementBody() && (KtPsiUtil.skipTrailingWhitespacesAndComments(
                    it
                ) as KtReturnExpression).returnedExpression.isTrivialStatementBody()
            },


            registerInspectionBasedProcessing(IfThenToSafeAccessInspection()),
            registerInspectionBasedProcessing(IfThenToSafeAccessInspection()),
            registerIntentionBasedProcessing(IfThenToElvisIntention()),
            registerInspectionBasedProcessing(SimplifyNegatedBinaryExpressionInspection()),
            registerInspectionBasedProcessing(ReplaceGetOrSetInspection()),
            registerIntentionBasedProcessing(AddOperatorModifierIntention()),
            registerIntentionBasedProcessing(ObjectLiteralToLambdaIntention()),
            registerIntentionBasedProcessing(AnonymousFunctionToLambdaIntention()),
            registerIntentionBasedProcessing(RemoveUnnecessaryParenthesesIntention()),
            registerIntentionBasedProcessing(DestructureIntention()),
            registerInspectionBasedProcessing(SimplifyAssertNotNullInspection()),
            registerIntentionBasedProcessing(RemoveRedundantCallsOfConversionMethodsIntention()),
            registerGeneralInspectionBasedProcessing(LiftReturnOrAssignmentInspection()),
            registerGeneralInspectionBasedProcessing(MayBeConstantInspection()),
            registerIntentionBasedProcessing(RemoveEmptyPrimaryConstructorIntention()),
            registerDiagnosticBasedProcessing(Errors.PLATFORM_CLASS_MAPPED_TO_KOTLIN) { element: KtDotQualifiedExpression, diagnostic ->
                val parent = element.parent as? KtImportDirective ?: return@registerDiagnosticBasedProcessing
                parent.delete()
            },

            registerDiagnosticBasedProcessing(
                Errors.UNSAFE_CALL,
                Errors.UNSAFE_INFIX_CALL,
                Errors.UNSAFE_OPERATOR_CALL
            ) { element: PsiElement, diagnostic ->
                val action =
                    AddExclExclCallFix.createActions(diagnostic).singleOrNull()
                        ?: return@registerDiagnosticBasedProcessing
                action.invoke(element.project, null, element.containingFile)
            },

            registerDiagnosticBasedProcessing(Errors.SMARTCAST_IMPOSSIBLE) { element: PsiElement, diagnostic ->
                val action =
                    SmartCastImpossibleExclExclFixFactory.createActions(diagnostic).singleOrNull()
                        ?: return@registerDiagnosticBasedProcessing
                action.invoke(element.project, null, element.containingFile)
            },

            registerDiagnosticBasedProcessing(Errors.TYPE_MISMATCH) { element: PsiElement, diagnostic ->
                val diagnosticWithParameters = diagnostic as? DiagnosticWithParameters2<KtExpression, KotlinType, KotlinType>
                    ?: return@registerDiagnosticBasedProcessing
                val expectedType = diagnosticWithParameters.a
                val realType = diagnosticWithParameters.b
                if (realType.makeNotNullable().isSubtypeOf(expectedType.makeNotNullable())
                    && realType.isNullable()
                    && !expectedType.isNullable()
                ) {
                    val factory = KtPsiFactory(element)
                    element.replace(factory.createExpressionByPattern("($0)!!", element.text))
                }
            },

            registerDiagnosticBasedProcessing(Errors.CAST_NEVER_SUCCEEDS) { element: KtSimpleNameExpression, diagnostic ->
                val action =
                    ReplacePrimitiveCastWithNumberConversionFix.createActionsForAllProblems(listOf(diagnostic)).singleOrNull()
                        ?: return@registerDiagnosticBasedProcessing
                action.invoke(element.project, null, element.containingKtFile)
            },
            registerDiagnosticBasedProcessing(Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE) { element: KtNamedDeclaration, diagnostic ->
                val action =
                    ChangeCallableReturnTypeFix.ReturnTypeMismatchOnOverrideFactory
                        .createActionsForAllProblems(listOf(diagnostic)).singleOrNull()
                        ?: return@registerDiagnosticBasedProcessing
                action.invoke(element.project, null, element.containingKtFile)
            },

            RemoveRedundantTypeQualifierProcessing(),
            RemoveRedundantExpressionQualifierProcessing(),

            registerDiagnosticBasedProcessing<KtBinaryExpressionWithTypeRHS>(Errors.USELESS_CAST) { element, _ ->
                if (element.left.isNullExpression()) return@registerDiagnosticBasedProcessing
                val expression = RemoveUselessCastFix.invoke(element)

                val variable = expression.parent as? KtProperty
                if (variable != null && expression == variable.initializer && variable.isLocal) {
                    val ref = ReferencesSearch.search(variable, LocalSearchScope(variable.containingFile)).findAll().singleOrNull()
                    if (ref != null && ref.element is KtSimpleNameExpression) {
                        ref.element.replace(expression)
                        variable.delete()
                    }
                }
            },

            registerDiagnosticBasedProcessing<KtTypeProjection>(Errors.REDUNDANT_PROJECTION) { _, diagnostic ->
                val fix = RemoveModifierFix.createRemoveProjectionFactory(true).createActions(diagnostic).single() as RemoveModifierFix
                fix.invoke()
            },

            registerDiagnosticBasedProcessing<KtModifierListOwner>(Errors.VIRTUAL_MEMBER_HIDDEN) { element, diagnostic ->
                val action = AddModifierFix
                    .createFactory(KtTokens.OVERRIDE_KEYWORD)
                    .createActions(diagnostic)
                    .singleOrNull() ?: return@registerDiagnosticBasedProcessing
                action.invoke(element.project, null, element.containingKtFile)
            },

            registerDiagnosticBasedProcessing<KtModifierListOwner>(Errors.NON_FINAL_MEMBER_IN_FINAL_CLASS) { _, diagnostic ->
                val fix =
                    RemoveModifierFix
                        .createRemoveModifierFromListOwnerFactory(KtTokens.OPEN_KEYWORD)
                        .createActions(diagnostic).single() as RemoveModifierFix
                fix.invoke()
            },
            registerDiagnosticBasedProcessing<KtModifierListOwner>(Errors.NON_FINAL_MEMBER_IN_OBJECT) { _, diagnostic ->
                val fix =
                    RemoveModifierFix
                        .createRemoveModifierFromListOwnerFactory(KtTokens.OPEN_KEYWORD)
                        .createActions(diagnostic).single() as RemoveModifierFix
                fix.invoke()
            },

            registerDiagnosticBasedProcessingFactory(
                Errors.VAL_REASSIGNMENT, Errors.CAPTURED_VAL_INITIALIZATION, Errors.CAPTURED_MEMBER_VAL_INITIALIZATION
            ) { element: KtSimpleNameExpression, _: Diagnostic ->
                val property = element.mainReference.resolve() as? KtProperty
                if (property == null) {
                    null
                } else {
                    val action = {
                        if (!property.isVar) {
                            property.valOrVarKeyword.replace(KtPsiFactory(element.project).createVarKeyword())
                        }
                    }
                    action
                }
            },

            registerDiagnosticBasedProcessing<KtSimpleNameExpression>(Errors.UNNECESSARY_NOT_NULL_ASSERTION) { element, _ ->
                val exclExclExpr = element.parent as KtUnaryExpression
                val baseExpression = exclExclExpr.baseExpression!!
                val context = baseExpression.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
                if (context.diagnostics.forElement(element).any { it.factory == Errors.UNNECESSARY_NOT_NULL_ASSERTION }) {
                    exclExclExpr.replace(baseExpression)
                }
            },
            RemoveForExpressionLoopParameterTypeProcessing()
        )
    )

    val processings: Collection<NewJ2kPostProcessing> =
        mainProcessings.processings().toList()


    init {
        processingsToPriorityMap.putAll(processings.mapToIndex())
    }


    private inline fun <reified TElement : PsiElement, TIntention : SelfTargetingRangeIntention<TElement>> registerIntentionBasedProcessing(
        intention: TIntention,
        noinline additionalChecker: (TElement) -> Boolean = { true }
    ) = object : NewJ2kPostProcessing {
        // Intention can either need or not need write action
        override val writeActionNeeded = intention.startInWriteAction()

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (!TElement::class.java.isInstance(element)) return null
            val tElement = element as TElement
            if (intention.applicabilityRange(tElement) == null) return null
            if (!additionalChecker(tElement)) return null
            return {
                if (intention.applicabilityRange(tElement) != null) { // check availability of the intention again because something could change
                    intention.applyTo(element, null)
                }
            }
        }
    }


    private inline fun <TInspection : AbstractKotlinInspection> registerGeneralInspectionBasedProcessing(
        inspection: TInspection,
        acceptInformationLevel: Boolean = false
    ) = (object : NewJ2kPostProcessing {
        override val writeActionNeeded = false

        fun <D : CommonProblemDescriptor> QuickFix<D>.applyFixSmart(project: Project, descriptor: D) {
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
                        if (!action.isAvailable(project, null, descriptor.psiElement.containingFile)) return
                        action.invoke(project, null, descriptor.psiElement.containingFile)
                    }


                    @Suppress("NOT_YET_SUPPORTED_IN_INLINE")
                    fun applyIntention() {
                        val action = this.action
                        when (action) {
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
                this.applyFix(project, descriptor)
            }
        }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
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
                        .forEach { it.fixes?.firstOrNull()?.applyFixSmart(element.project, it) }
                }
            }
        }
    })


    private inline fun
            <reified TElement : PsiElement,
                    TInspection : AbstractApplicabilityBasedInspection<TElement>> registerInspectionBasedProcessing(

        inspection: TInspection,
        acceptInformationLevel: Boolean = false
    ) = object : NewJ2kPostProcessing {
        // Inspection can either need or not need write action
        override val writeActionNeeded = inspection.startFixInWriteAction

        private fun isApplicable(element: TElement): Boolean {
            if (!inspection.isApplicable(element)) return false
            return acceptInformationLevel || inspection.inspectionHighlightType(element) != ProblemHighlightType.INFORMATION
        }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (!TElement::class.java.isInstance(element)) return null
            val tElement = element as TElement
            if (!isApplicable(tElement)) return null
            return {
                if (isApplicable(tElement)) { // check availability of the inspection again because something could change
                    inspection.applyTo(inspection.inspectionTarget(tElement))
                }
            }
        }
    }


    private inline fun <reified TElement : PsiElement> registerDiagnosticBasedProcessing(
        vararg diagnosticFactory: DiagnosticFactory<*>,
        crossinline fix: (TElement, Diagnostic) -> Unit
    ) = object : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (!TElement::class.java.isInstance(element)) return null
            val diagnostic = diagnostics.forElement(element).firstOrNull { it.factory in diagnosticFactory } ?: return null
            return {
                fix(element as TElement, diagnostic)
            }
        }
    }

    private inline fun <reified TElement : PsiElement> registerDiagnosticBasedProcessingFactory(
        vararg diagnosticFactory: DiagnosticFactory<*>,
        crossinline fixFactory: (TElement, Diagnostic) -> (() -> Unit)?
    ) = object : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (!TElement::class.java.isInstance(element)) return null
            val diagnostic = diagnostics.forElement(element).firstOrNull { it.factory in diagnosticFactory } ?: return null
            return fixFactory(element as TElement, diagnostic)
        }
    }


    private class RemoveExplicitPropertyType : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics, settings: ConverterSettings?): (() -> Unit)? {
            if (element !is KtProperty) return null
            val needFieldTypes = settings?.specifyFieldTypeByDefault == true
            val needLocalVariablesTypes = settings?.specifyLocalVariableTypeByDefault == true

            fun check(element: KtProperty): Boolean {
                if (needLocalVariablesTypes && element.isLocal) return false
                if (needFieldTypes && element.isMember) return false
                val initializer = element.initializer ?: return false
                val withoutExpectedType = initializer.analyzeInContext(initializer.getResolutionScope())
                val descriptor = element.resolveToDescriptorIfAny() as? CallableDescriptor ?: return false
                return when (withoutExpectedType.getType(initializer)) {
                    descriptor.returnType -> true
                    descriptor.returnType?.makeNotNullable() -> !element.isVar
                    else -> false
                }
            }

            if (!check(element)) {
                return null
            } else {
                return {
                    if (element.isValid && check(element)) {
                        element.typeReference = null
                    }
                }
            }
        }
    }


    private class RemoveRedundantNullability : NewJ2kPostProcessing {
        override val writeActionNeeded: Boolean = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtProperty) return null

            fun check(element: KtProperty): Boolean {
                if (!element.isLocal) return false
                val typeReference = element.typeReference
                if (typeReference == null || typeReference.typeElement !is KtNullableType) return false
                val initializerType = element.initializer?.let {
                    it.analyzeInContext(element.getResolutionScope()).getType(it)
                }
                if (initializerType?.isNullable() == true) return false

                return ReferencesSearch.search(element, element.useScope).findAll().mapNotNull { ref ->
                    val parent = (ref.element.parent as? KtExpression)?.asAssignment()
                    parent?.takeIf { it.left == ref.element }
                }.all {
                    val right = it.right
                    val withoutExpectedType = right?.analyzeInContext(element.getResolutionScope())
                    withoutExpectedType?.getType(right)?.isNullable() == false
                }
            }

            if (!check(element)) {
                return null
            } else {
                return {
                    val typeElement = element.typeReference?.typeElement
                    if (element.isValid && check(element) && typeElement != null && typeElement is KtNullableType) {
                        typeElement.replace(typeElement.innerType!!)
                    }
                }
            }
        }
    }

    private class RemoveExplicitTypeArgumentsProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtTypeArgumentList || !RemoveExplicitTypeArgumentsIntention.isApplicableTo(
                    element,
                    approximateFlexible = true
                )
            ) return null

            return {
                if (RemoveExplicitTypeArgumentsIntention.isApplicableTo(element, approximateFlexible = true)) {
                    element.delete()
                }
            }
        }
    }

    private class RemoveRedundantOverrideVisibilityProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallableDeclaration || !element.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return null
            val modifier = element.visibilityModifierType() ?: return null
            return { element.setVisibility(modifier) }
        }
    }

    private class ConvertToStringTemplateProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        private val intention = ConvertToStringTemplateIntention()

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element is KtBinaryExpression && intention.isApplicableTo(element) && ConvertToStringTemplateIntention.shouldSuggestToConvert(
                    element
                )
            ) {
                return { intention.applyTo(element, null) }
            } else {
                return null
            }
        }
    }

    private class UsePropertyAccessSyntaxProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        private val intention = UsePropertyAccessSyntaxIntention()

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallExpression) return null
            val propertyName = intention.detectPropertyNameToUse(element) ?: return null
            return { intention.applyTo(element, propertyName, reformat = true) }
        }
    }

    private class RemoveRedundantSamAdaptersProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallExpression) return null

            val expressions = RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
            if (expressions.isEmpty()) return null

            return {
                RedundantSamConstructorInspection.samConstructorCallsToBeConverted(element)
                    .forEach { RedundantSamConstructorInspection.replaceSamConstructorCall(it) }
            }
        }
    }

    private class UseExpressionBodyProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtPropertyAccessor) return null

            val inspection = UseExpressionBodyInspection(convertEmptyToUnit = false)
            if (!inspection.isActiveFor(element)) return null

            return {
                if (inspection.isActiveFor(element)) {
                    inspection.simplify(element, false)
                }
            }
        }
    }

    private class RemoveRedundantCastToNullableProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtBinaryExpressionWithTypeRHS) return null

            val context = element.analyze()
            val leftType = context.getType(element.left) ?: return null
            val rightType = context.get(BindingContext.TYPE, element.right) ?: return null

            if (!leftType.isMarkedNullable && rightType.isMarkedNullable) {
                return {
                    val type = element.right?.typeElement as? KtNullableType
                    type?.replace(type.innerType!!)
                }
            }

            return null
        }
    }

    private class FixObjectStringConcatenationProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtBinaryExpression ||
                element.operationToken != KtTokens.PLUS ||
                diagnostics.forElement(element.operationReference).none {
                    it.factory == Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER
                            || it.factory == Errors.NONE_APPLICABLE
                }
            )
                return null

            val bindingContext = element.analyze()
            val rightType = element.right?.getType(bindingContext) ?: return null

            if (KotlinBuiltIns.isString(rightType)) {
                return {
                    val factory = KtPsiFactory(element)
                    element.left!!.replace(factory.buildExpression {
                        appendFixedText("(")
                        appendExpression(element.left)
                        appendFixedText(").toString()")
                    })
                }
            }
            return null
        }
    }

    private class UninitializedVariableReferenceFromInitializerToThisReferenceProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtSimpleNameExpression || diagnostics.forElement(element).none { it.factory == Errors.UNINITIALIZED_VARIABLE }) return null

            val resolved = element.mainReference.resolve() ?: return null
            if (resolved.isAncestor(element, strict = true)) {
                if (resolved is KtVariableDeclaration && resolved.hasInitializer()) {
                    val anonymousObject = element.getParentOfType<KtClassOrObject>(true) ?: return null
                    if (resolved.initializer!!.getChildOfType<KtClassOrObject>() == anonymousObject) {
                        return { element.replaced(KtPsiFactory(element).createThisExpression()) }
                    }
                }
            }

            return null
        }
    }

    private class UnresolvedVariableReferenceFromInitializerToThisReferenceProcessing : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtSimpleNameExpression || diagnostics.forElement(element).none { it.factory == Errors.UNRESOLVED_REFERENCE }) return null

            val anonymousObject = element.getParentOfType<KtClassOrObject>(true) ?: return null

            val variable = anonymousObject.getParentOfType<KtVariableDeclaration>(true) ?: return null

            if (variable.nameAsName == element.getReferencedNameAsName() &&
                variable.initializer?.getChildOfType<KtClassOrObject>() == anonymousObject
            ) {
                return { element.replaced(KtPsiFactory(element).createThisExpression()) }
            }

            return null
        }
    }

    private class VarToVal : NewJ2kPostProcessing {
        override val writeActionNeeded = true

        private fun KtProperty.hasWriteUsages(): Boolean =
            ReferencesSearch.search(this, useScope).any { usage ->
                (usage as? KtSimpleNameReference)?.element?.let {
                    it.readWriteAccess(useResolveForReadWrite = true).isWrite
                            && it.parentOfType<KtAnonymousInitializer>() == null//TODO properly check
                } == true
            }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtProperty) return null

            fun check(element: KtProperty): Boolean {
                if (!element.isVar) return false
                if (!element.isPrivate()) return false
                return !element.hasWriteUsages()
            }

            if (!check(element)) {
                return null
            } else {
                return {
                    if (element.isValid && check(element)) {
                        val factory = KtPsiFactory(element)
                        element.valOrVarKeyword.replace(factory.createValKeyword())
                        println()
                    }
                }
            }
        }
    }

    private class JavaObjectEqualsToEqOperator : NewJ2kPostProcessing {
        companion object {
            private val CALL_FQ_NAME = FqName("java.util.Objects.equals")
        }

        private fun check(callExpression: KtCallExpression): Boolean {
            if (callExpression.valueArguments.size != 2) return false
            if (callExpression.valueArguments.any { it.getArgumentExpression() == null }) return false
            val target = callExpression.calleeExpression
                .safeAs<KtReferenceExpression>()
                ?.resolve()
                ?: return false
            return target.getKotlinFqName() == CALL_FQ_NAME
        }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtCallExpression) return null
            if (!check(element)) return null
            return {
                if (element.isValid() && check(element)) {
                    val factory = KtPsiFactory(element)
                    element.getQualifiedExpressionForSelectorOrThis().replace(
                        factory.createExpressionByPattern(
                            "($0 == $1)",
                            element.valueArguments[0].getArgumentExpression()!!,
                            element.valueArguments[1].getArgumentExpression()!!
                        )
                    )
                }
            }
        }

        override val writeActionNeeded: Boolean = true
    }

    private class RemoveForExpressionLoopParameterTypeProcessing : CheckableProcessing<KtForExpression>(KtForExpression::class) {
        override fun check(element: KtForExpression, settings: ConverterSettings?): Boolean =
            element.loopParameter?.typeReference?.typeElement != null
                    && settings?.specifyLocalVariableTypeByDefault != true

        override fun action(element: KtForExpression) {
            element.loopParameter?.typeReference = null
        }

    }

    private class RemoveRedundantExpressionQualifierProcessing : NewJ2kPostProcessing {
        private fun check(qualifiedExpression: KtQualifiedExpression): Boolean {
            val qualifier = (qualifiedExpression.receiverExpression as? KtNameReferenceExpression)
                ?.referenceExpression()
                ?.resolve()?.safeAs<KtClassOrObject>()?.parentClassForCompanionOrThis() ?: return false
            return qualifier.parentClassForCompanionOrThis() in qualifiedExpression.parentsOfType<KtClassOrObject>()
        }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtQualifiedExpression) return null
            if (!check(element)) return null
            return {
                if (element.isValid() && check(element)) {
                    element.replace(element.selectorExpression!!)
                }
            }
        }

        override val writeActionNeeded: Boolean = true
    }

    private class RemoveRedundantTypeQualifierProcessing : NewJ2kPostProcessing {
        private fun check(reference: KtUserType): Boolean {
            val qualifierClass = reference.qualifier
                ?.referenceExpression
                ?.resolve() as? KtClassOrObject ?: return false
            val topLevelClass = reference.topLevelContainingClassOrObject() ?: return false
            return topLevelClass.isAncestor(qualifierClass)
        }

        override fun createAction(element: PsiElement, diagnostics: Diagnostics): (() -> Unit)? {
            if (element !is KtUserType) return null
            if (!check(element)) return null
            return {
                if (element.isValid && check(element)) {
                    element.deleteQualifier()
                }
            }
        }

        override val writeActionNeeded: Boolean = true
    }

    private class RemoveRedundantConstructorKeywordProcessing : CheckableProcessing<KtPrimaryConstructor>(KtPrimaryConstructor::class) {
        override fun check(element: KtPrimaryConstructor): Boolean =
            element.containingClassOrObject is KtClass
                    && element.getConstructorKeyword() != null
                    && element.annotationEntries.isEmpty()
                    && element.visibilityModifier() == null


        override fun action(element: KtPrimaryConstructor) {
            element.removeRedundantConstructorKeywordAndSpace()
        }
    }

    private class RemoveRedundantModalityModifierProcessing : CheckableProcessing<KtDeclaration>(KtDeclaration::class) {
        override fun check(element: KtDeclaration): Boolean {
            val modalityModifier = element.modalityModifier() ?: return false
            val modalityModifierType = modalityModifier.node.elementType
            val implicitModality = element.implicitModality()

            return modalityModifierType == implicitModality
        }

        override fun action(element: KtDeclaration) {
            element.removeModifierSmart(element.modalityModifierType()!!)
        }
    }

    //hack until KT-30804 is fixed
    private fun KtModifierListOwner.removeModifierSmart(modifierToken: KtModifierKeywordToken) {
        val modifier = modifierList?.getModifier(modifierToken)
        val comment = modifier?.getPrevSiblingIgnoringWhitespace() as? PsiComment
        comment?.also {
            it.parent.addAfter(KtPsiFactory(this).createNewLine(), it)
        }
        val newElement = copy() as KtModifierListOwner
        newElement.removeModifier(modifierToken)
        replace(newElement)
        containingFile.commitAndUnblockDocument()
    }

    private class RemoveRedundantVisibilityModifierProcessing : CheckableProcessing<KtDeclaration>(KtDeclaration::class) {
        override fun check(element: KtDeclaration): Boolean {
            val visibilityModifier = element.visibilityModifier() ?: return false
            val implicitVisibility = element.implicitVisibility()
            return when {
                visibilityModifier.node.elementType == implicitVisibility ->
                    true
                element.hasModifier(KtTokens.INTERNAL_KEYWORD) && element.containingClassOrObject?.isLocal == true ->
                    true
                else -> false
            }
        }

        override fun action(element: KtDeclaration) {
            element.removeModifierSmart(element.visibilityModifierType()!!)
        }
    }

}