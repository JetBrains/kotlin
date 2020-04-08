/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiVariable
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.statistics.JavaStatisticsManager
import com.intellij.refactoring.rename.NameSuggestionProvider
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinNameSuggestionProvider : NameSuggestionProvider {
    override fun getSuggestedNames(
        element: PsiElement,
        nameSuggestionContext: PsiElement?,
        result: MutableSet<String>
    ): SuggestedNameInfo? {
        if (element is KtCallableDeclaration) {
            val context = nameSuggestionContext ?: element.parent
            val target = if (element is KtProperty || element is KtParameter) {
                NewDeclarationNameValidator.Target.VARIABLES
            } else {
                NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES
            }
            val validator = NewDeclarationNameValidator(context, element, target, listOf(element))
            val names = SmartList<String>().apply {
                val name = element.name
                if (!name.isNullOrBlank()) {
                    this += KotlinNameSuggester.getCamelNames(name, validator, name.first().isLowerCase())
                }

                val callableDescriptor = element.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL) as CallableDescriptor
                val type = callableDescriptor.returnType
                if (type != null && !type.isUnit() && !KotlinBuiltIns.isPrimitiveType(type)) {
                    this += KotlinNameSuggester.suggestNamesByType(type, validator)
                }
            }
            result += names

            if (element is KtProperty && element.isLocal) {
                for (ref in ReferencesSearch.search(element, LocalSearchScope(element.parent))) {
                    val refExpr = ref.element as? KtSimpleNameExpression ?: continue
                    val argument = refExpr.parent as? KtValueArgument ?: continue
                    val callElement = (argument.parent as? KtValueArgumentList)?.parent as? KtCallElement ?: continue
                    val resolvedCall = callElement.resolveToCall() ?: continue
                    val parameterName = (resolvedCall.getArgumentMapping(argument) as? ArgumentMatch)?.valueParameter?.name ?: continue
                    result += parameterName.asString()
                }
            }

            return object : SuggestedNameInfo(names.toTypedArray()) {
                override fun nameChosen(name: String?) {
                    val psiVariable = element.toLightElements().firstIsInstanceOrNull<PsiVariable>() ?: return
                    JavaStatisticsManager.incVariableNameUseCount(
                        name,
                        JavaCodeStyleManager.getInstance(element.project).getVariableKind(psiVariable),
                        psiVariable.name,
                        psiVariable.type
                    )
                }
            }
        }

        return null
    }
}
