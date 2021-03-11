/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DataClassDescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.util.*

abstract class ChangeCallableReturnTypeFix(
    element: KtCallableDeclaration,
    type: KotlinType
) : KotlinQuickFixAction<KtCallableDeclaration>(element) {

    private val changeFunctionLiteralReturnTypeFix: ChangeFunctionLiteralReturnTypeFix?

    private val typeContainsError = ErrorUtils.containsErrorType(type)
    private val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_TYPES_WITH_SHORT_NAMES.renderType(type)
    private val typeSourceCode = IdeDescriptorRenderers.SOURCE_CODE_TYPES.renderType(type)
    private val isUnitType = type.isUnit()

    init {
        changeFunctionLiteralReturnTypeFix = if (element is KtFunctionLiteral) {
            val functionLiteralExpression = PsiTreeUtil.getParentOfType(element, KtLambdaExpression::class.java)
                ?: error("FunctionLiteral outside any FunctionLiteralExpression: " + element.getElementTextWithContext())
            ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, type)
        } else {
            null
        }
    }

    open fun functionPresentation(): String? {
        val element = element!!
        if (element.name == null) return null
        val container = element.unsafeResolveToDescriptor().containingDeclaration as? ClassDescriptor
        val containerName = container?.name?.takeUnless { it.isSpecial }
        return StringPresentation.functionOrConstructorParameterPresentation(element, containerName)
    }

    class OnType(element: KtFunction, type: KotlinType) : ChangeCallableReturnTypeFix(element, type), HighPriorityAction {
        override fun functionPresentation() = null
    }

    class ForEnclosing(element: KtFunction, type: KotlinType) : ChangeCallableReturnTypeFix(element, type), HighPriorityAction {
        override fun functionPresentation(): String? {
            val presentation = super.functionPresentation()
                ?: return KotlinBundle.message("fix.change.return.type.presentation.enclosing.function")
            return KotlinBundle.message("fix.change.return.type.presentation.enclosing", presentation)
        }
    }

    class ForCalled(element: KtCallableDeclaration, type: KotlinType) : ChangeCallableReturnTypeFix(element, type) {
        override fun functionPresentation(): String? {
            val presentation = super.functionPresentation()
                ?: return KotlinBundle.message("fix.change.return.type.presentation.called.function")
            return when (element) {
                is KtParameter -> KotlinBundle.message("fix.change.return.type.presentation.accessed", presentation)
                else -> KotlinBundle.message("fix.change.return.type.presentation.called", presentation)
            }
        }
    }

    class ForOverridden(element: KtFunction, type: KotlinType) : ChangeCallableReturnTypeFix(element, type) {
        override fun functionPresentation(): String? {
            val presentation = super.functionPresentation() ?: return null
            return StringPresentation.baseFunctionOrConstructorParameterPresentation(presentation)
        }
    }

    override fun getText(): String {
        val element = element ?: return ""

        if (changeFunctionLiteralReturnTypeFix != null) {
            return changeFunctionLiteralReturnTypeFix.text
        }

        return StringPresentation.getTextForQuickFix(element, functionPresentation(), isUnitType, typePresentation)
    }

    override fun getFamilyName(): String = StringPresentation.familyName()

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        return !typeContainsError &&
                element !is KtConstructor<*>
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return

        if (changeFunctionLiteralReturnTypeFix != null) {
            changeFunctionLiteralReturnTypeFix.invoke(project, editor!!, file)
        } else {
            if (!(isUnitType && element is KtFunction && element.hasBlockBody())) {
                var newTypeRef = KtPsiFactory(project).createType(typeSourceCode)
                newTypeRef = element.setTypeReference(newTypeRef)!!
                ShortenReferences.DEFAULT.process(newTypeRef)
            } else {
                element.typeReference = null
            }
        }
    }

    object ComponentFunctionReturnTypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val entry = getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic)
            val context = entry.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry) ?: return null
            val componentFunction =
                DescriptorToSourceUtils.descriptorToDeclaration(resolvedCall.candidateDescriptor) as? KtCallableDeclaration
                    ?: return null
            val expectedType = context[BindingContext.TYPE, entry.typeReference!!] ?: return null
            return ForCalled(componentFunction, expectedType)
        }
    }

    object HasNextFunctionTypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = QuickFixUtil.getParentElementOfType(diagnostic, KtExpression::class.java)
                ?: error("HAS_NEXT_FUNCTION_TYPE_MISMATCH reported on element that is not within any expression")
            val context = expression.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = context[BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression] ?: return null
            val hasNextDescriptor = resolvedCall.candidateDescriptor
            val hasNextFunction = DescriptorToSourceUtils.descriptorToDeclaration(hasNextDescriptor) as KtFunction? ?: return null
            return ForCalled(hasNextFunction, hasNextDescriptor.builtIns.booleanType)
        }
    }

    object CompareToTypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = QuickFixUtil.getParentElementOfType(diagnostic, KtBinaryExpression::class.java)
                ?: error("COMPARE_TO_TYPE_MISMATCH reported on element that is not within any expression")
            val resolvedCall = expression.resolveToCall() ?: return null
            val compareToDescriptor = resolvedCall.candidateDescriptor
            val compareTo = DescriptorToSourceUtils.descriptorToDeclaration(compareToDescriptor) as? KtFunction ?: return null
            return ForCalled(compareTo, compareToDescriptor.builtIns.intType)
        }
    }

    object ReturnTypeMismatchOnOverrideFactory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return emptyList()

            val actions = LinkedList<IntentionAction>()

            val descriptor = function.resolveToDescriptorIfAny(BodyResolveMode.FULL) as? FunctionDescriptor ?: return emptyList()

            val matchingReturnType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(descriptor)
            if (matchingReturnType != null) {
                actions.add(OnType(function, matchingReturnType))
            }

            val functionType = descriptor.returnType ?: return actions

            val overriddenMismatchingFunctions = LinkedList<FunctionDescriptor>()
            for (overriddenFunction in descriptor.overriddenDescriptors) {
                val overriddenFunctionType = overriddenFunction.returnType ?: continue
                if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(functionType, overriddenFunctionType)) {
                    overriddenMismatchingFunctions.add(overriddenFunction)
                }
            }

            if (overriddenMismatchingFunctions.size == 1) {
                val overriddenFunction = DescriptorToSourceUtils.descriptorToDeclaration(overriddenMismatchingFunctions[0])
                if (overriddenFunction is KtFunction) {
                    actions.add(ForOverridden(overriddenFunction, functionType))
                }
            }

            return actions
        }
    }

    object ChangingReturnTypeToUnitFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return null
            return ForEnclosing(function, function.builtIns.unitType)
        }
    }

    object ChangingReturnTypeToNothingFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction::class.java) ?: return null
            return ForEnclosing(function, function.builtIns.nothingType)
        }
    }

    companion object {
        fun getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic: Diagnostic): KtDestructuringDeclarationEntry {
            val componentName = COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.cast(diagnostic).a
            val componentIndex = DataClassDescriptorResolver.getComponentIndex(componentName.asString())
            val multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, KtDestructuringDeclaration::class.java)
                ?: error("COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH reported on expression that is not within any multi declaration")
            return multiDeclaration.entries[componentIndex - 1]
        }
    }

    object StringPresentation {
        fun familyName(): String = KotlinBundle.message("fix.change.return.type.family")

        fun functionOrConstructorParameterPresentation(element: KtCallableDeclaration, containerName: Name?): String? {
            val name = element.name
            return if (name != null) {
                val fullName = if (containerName != null) "'${containerName.asString()}.$name'" else "'$name'"
                when (element) {
                    is KtParameter -> KotlinBundle.message("fix.change.return.type.presentation.property", fullName)
                    is KtProperty -> KotlinBundle.message("fix.change.return.type.presentation.property", fullName)
                    else -> KotlinBundle.message("fix.change.return.type.presentation.function", fullName)
                }
            } else null
        }


        fun baseFunctionOrConstructorParameterPresentation(presentation: String): String =
            KotlinBundle.message("fix.change.return.type.presentation.base", presentation)

        fun baseFunctionOrConstructorParameterPresentation(element: KtCallableDeclaration, containerName: Name?): String? {
            val presentation = functionOrConstructorParameterPresentation(element, containerName) ?: return null
            return baseFunctionOrConstructorParameterPresentation(presentation)
        }

        fun getTextForQuickFix(
            element: KtCallableDeclaration,
            presentation: String?,
            isUnitType: Boolean,
            typePresentation: String
        ): String {
            if (isUnitType && element is KtFunction && element.hasBlockBody()) {
                return if (presentation == null)
                    KotlinBundle.message("fix.change.return.type.remove.explicit.return.type")
                else
                    KotlinBundle.message("fix.change.return.type.remove.explicit.return.type.of", presentation)
            }

            return when (element) {
                is KtFunction -> {
                    if (presentation != null)
                        KotlinBundle.message("fix.change.return.type.return.type.text.of", presentation, typePresentation)
                    else
                        KotlinBundle.message("fix.change.return.type.return.type.text", typePresentation)
                }
                else -> {
                    if (presentation != null)
                        KotlinBundle.message("fix.change.return.type.type.text.of", presentation, typePresentation)
                    else
                        KotlinBundle.message("fix.change.return.type.type.text", typePresentation)
                }
            }
        }
    }
}

