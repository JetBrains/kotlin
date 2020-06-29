/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createDestructuringDeclarationByPattern
import org.jetbrains.kotlin.psi.destructuringDeclarationVisitor

class IncompleteDestructuringInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return destructuringDeclarationVisitor(fun(destructuringDeclaration) {
            val initializer = destructuringDeclaration.initializer ?: return
            val type = initializer.analyze().getType(initializer) ?: return

            val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return

            val primaryParameterNames = classDescriptor.constructors
                .firstOrNull { it.isPrimary }
                ?.valueParameters
                ?.map { it.name.asString() } ?: return

            if (destructuringDeclaration.entries.size < primaryParameterNames.size) {
                val rPar = destructuringDeclaration.rPar ?: return
                holder.registerProblem(
                    rPar,
                    KotlinBundle.message("incomplete.destructuring.declaration.text"),
                    IncompleteDestructuringQuickfix(primaryParameterNames)
                )
            }
        })
    }
}

class IncompleteDestructuringQuickfix(private val primaryParameterNames: List<String>) : LocalQuickFix {
    override fun getFamilyName() = KotlinBundle.message("incomplete.destructuring.fix.family.name")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val destructuringDeclaration = element.parent as? KtDestructuringDeclaration ?: return
        if (destructuringDeclaration.entries.size >= primaryParameterNames.size) return

        val namesToAdd =
            primaryParameterNames.subList(destructuringDeclaration.entries.size, primaryParameterNames.size)
        val names = destructuringDeclaration.entries.mapNotNull { it.name }.toMutableList() + namesToAdd
        val joinedNames = names.joinToString()

        val initializer = destructuringDeclaration.initializer ?: return
        val factory = KtPsiFactory(destructuringDeclaration)

        val newDestructuringDeclaration = factory.createDestructuringDeclarationByPattern(
            if (destructuringDeclaration.isVar) "var ($0) = $1" else "val ($0) = $1",
            joinedNames, initializer
        )

        element.parent.replace(newDestructuringDeclaration)
    }
}
