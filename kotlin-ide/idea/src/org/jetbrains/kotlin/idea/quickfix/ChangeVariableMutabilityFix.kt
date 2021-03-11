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

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable.CreatePropertyDelegateAccessorsActionFactory
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

class ReassignmentActionFactory(val factory: DiagnosticFactory1<*, DeclarationDescriptor>) : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val propertyDescriptor = factory.cast(diagnostic).a
        val declaration =
            DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor) as? KtValVarKeywordOwner ?: return null
        return ChangeVariableMutabilityFix(declaration, true)
    }
}

// TODO: Move this to idea-fir-independent
object ConstValFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val (modifier, element) = Errors.WRONG_MODIFIER_TARGET.cast(diagnostic).run { a to psiElement }
        if (modifier != KtTokens.CONST_KEYWORD) return null
        val property = element.getStrictParentOfType<KtProperty>() ?: return null
        return ChangeVariableMutabilityFix(property, makeVar = false)
    }
}

object DelegatedPropertyValFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val element = Errors.DELEGATE_SPECIAL_FUNCTION_MISSING.cast(diagnostic).psiElement
        val property = element.getStrictParentOfType<KtProperty>() ?: return null
        val info = CreatePropertyDelegateAccessorsActionFactory.extractFixData(property, diagnostic).singleOrNull() ?: return null
        if (info.name != OperatorNameConventions.SET_VALUE.asString()) return null
        return ChangeVariableMutabilityFix(property, makeVar = false, actionText = KotlinBundle.message("change.to.val"))
    }
}