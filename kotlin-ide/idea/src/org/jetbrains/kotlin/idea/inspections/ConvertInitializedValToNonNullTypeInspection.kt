/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils

class ConvertInitializedValToNonNullTypeInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        propertyVisitor(fun(property) {
            val typeReference = property.typeReference ?: return

            if (shouldConvertToNonNullType(property)) {
                holder.registerProblem(
                    typeReference,
                    KotlinBundle.message("initialized.val.should.be.converted.to.non.null.type"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveRedundantNullableTypeQuickfix()
                )
            }
        })

    companion object {
        private fun shouldConvertToNonNullType(property: KtProperty): Boolean {
            val initializer = property.initializer ?: return false
            val type = property.resolveToDescriptorIfAny()?.type ?: return false
            if (!TypeUtils.isNullableType(type)) return false

            when (initializer) {
                is KtConstantExpression -> {
                    return !initializer.isNull()
                }
                is KtStringTemplateExpression -> {
                    return true
                }
                is KtNameReferenceExpression, is KtCallExpression -> {
                    val context = initializer.analyze(BodyResolveMode.PARTIAL)
                    val assignedType = initializer.getType(context) ?: return false
                    return !TypeUtils.isNullableType(assignedType)
                }
                else -> return false
            }
        }
    }
}

class RemoveRedundantNullableTypeQuickfix : LocalQuickFix {
    override fun getName() = KotlinBundle.message("convert.initialized.val.to.non.null.type.quick.fix.text")

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val factory = KtPsiFactory(project)
        element.replace(factory.createType(element.text.removeSuffix("?")))
    }
}
