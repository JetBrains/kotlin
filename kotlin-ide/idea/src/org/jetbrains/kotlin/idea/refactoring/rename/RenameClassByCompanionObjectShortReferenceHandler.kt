/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.project
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RenameClassByCompanionObjectShortReferenceHandler : AbstractReferenceSubstitutionRenameHandler() {
    override fun getElementToRename(dataContext: DataContext): PsiElement? {
        val refExpr = getReferenceExpression(dataContext) ?: return null
        val descriptor =
            refExpr.analyze(BodyResolveMode.PARTIAL)[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, refExpr] ?: return null
        return DescriptorToSourceUtilsIde.getAnyDeclaration(dataContext.project, descriptor)
    }
}