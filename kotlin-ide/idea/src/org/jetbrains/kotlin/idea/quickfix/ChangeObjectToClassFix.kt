/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class ChangeObjectToClassFix(element: KtObjectDeclaration) : KotlinQuickFixAction<KtObjectDeclaration>(element) {
    override fun getText(): String = KotlinBundle.message("fix.change.object.to.class")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val objectDeclaration = element ?: return
        val psiFactory = KtPsiFactory(objectDeclaration)
        objectDeclaration.getObjectKeyword()?.replace(psiFactory.createClassKeyword())
        objectDeclaration.replace(psiFactory.createClass(objectDeclaration.text))
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtObjectDeclaration>? {
            val element = diagnostic.psiElement as? KtConstructor<*> ?: return null
            val containingObject = element.containingClassOrObject as? KtObjectDeclaration ?: return null
            return ChangeObjectToClassFix(containingObject)
        }
    }
}
