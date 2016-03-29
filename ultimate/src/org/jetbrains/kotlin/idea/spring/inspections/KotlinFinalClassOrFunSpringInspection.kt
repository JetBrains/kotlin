/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.inspections

import com.intellij.codeInsight.highlighting.HighlightUsagesDescriptionLocation
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.psi.PsiElementVisitor
import com.intellij.spring.constants.SpringAnnotationsConstants
import com.intellij.spring.model.jam.stereotype.SpringComponent
import com.intellij.spring.model.jam.stereotype.SpringConfiguration
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.spring.isAnnotatedWith
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isInheritable
import org.jetbrains.kotlin.psi.psiUtil.isOverridable

class KotlinFinalClassOrFunSpringInspection : AbstractKotlinInspection() {
    class QuickFix<T: KtModifierListOwner>(private val element: T) : LocalQuickFix {
        override fun getName(): String {
            return "Make ${ElementDescriptionUtil.getElementDescription(element, HighlightUsagesDescriptionLocation.INSTANCE)} open"
        }

        override fun getFamilyName() = "Make declaration open"

        override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
            (element as? KtNamedDeclaration)?.containingClassOrObject?.addModifier(KtTokens.OPEN_KEYWORD)
            element.addModifier(KtTokens.OPEN_KEYWORD)
        }
    }

    private fun getMessage(declaration: KtNamedDeclaration): String? {
        when (declaration) {
            is KtClass -> {
                val lightClass = declaration.toLightClass() ?: return null
                when {
                    SpringConfiguration.META.getJamElement(lightClass) != null -> return "@Configuration class should be declared open"
                    SpringComponent.META.getJamElement(lightClass) != null -> return "@Component class should be declared open"
                }
            }

            is KtNamedFunction -> {
                val lightMethod = declaration.toLightMethods().firstOrNull() ?: return null
                if (lightMethod.isAnnotatedWith(SpringAnnotationsConstants.JAVA_SPRING_BEAN)) return "@Bean function should be declared open"
            }
        }
        return null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                when (declaration) {
                    is KtClass -> if (declaration.isInheritable()) return
                    is KtNamedFunction -> if (declaration.isOverridable()) return
                    else -> return
                }

                val message = getMessage(declaration) ?: return

                holder.registerProblem(
                        declaration.nameIdentifier ?: declaration,
                        message,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        QuickFix(declaration)
                )
            }
        }
    }
}