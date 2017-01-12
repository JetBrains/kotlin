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
import com.intellij.spring.model.jam.transaction.SpringTransactionalComponent
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.spring.isAnnotatedWith
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isInheritable
import org.jetbrains.kotlin.psi.psiUtil.isOverridable
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

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
            is KtClassOrObject -> {
                val lightClass = declaration.toLightClass() ?: return null
                val annotation = when {
                    SpringConfiguration.META.getJamElement(lightClass) != null -> "@Configuration"
                    SpringComponent.META.getJamElement(lightClass) != null -> "@Component"
                    SpringTransactionalComponent.META.getJamElement(lightClass) != null -> "@Transactional"
                    else -> return null
                }
                return if (declaration is KtClass) "$annotation class should be declared open" else "$annotation should not be applied to object declaration "
            }

            is KtNamedFunction -> {
                val lightMethod = declaration.toLightMethods().firstOrNull() ?: return null
                when {
                    lightMethod.isAnnotatedWith(SpringAnnotationsConstants.JAVA_SPRING_BEAN) -> return "@Bean function should be declared open"
                    lightMethod.isAnnotatedWith(SpringAnnotationsConstants.TRANSACTIONAL) -> return "@Transactional function should be declared open"
                }
            }
        }
        return null
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            private fun KtNamedDeclaration.isOpen(): Boolean {
                when (this) {
                    is KtClass -> if (isInheritable()) return true
                    is KtObjectDeclaration -> return false
                    is KtNamedFunction -> if (isOverridable()) return true
                }

                val descriptor = resolveToDescriptor(BodyResolveMode.PARTIAL) as? MemberDescriptor
                return descriptor?.modality != Modality.FINAL
            }

            override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                if (declaration.isOpen()) return

                val message = getMessage(declaration) ?: return

                val fixes = if (declaration !is KtObjectDeclaration) arrayOf(QuickFix(declaration)) else LocalQuickFix.EMPTY_ARRAY
                holder.registerProblem(
                        declaration.nameIdentifier ?: declaration,
                        message,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        *fixes
                )
            }
        }
    }
}