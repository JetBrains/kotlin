/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class SpecifySuperTypeFix(
    superExpression: KtSuperExpression,
    private val superTypes: List<String>
) : KotlinQuickFixAction<KtSuperExpression>(superExpression) {

    override fun getText() = "Specify supertype"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (editor == null) return
        val superExpression = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (superTypes.size == 1) {
                superExpression.specifySuperType(superTypes.first())
            } else {
                JBPopupFactory
                    .getInstance()
                    .createListPopup(createListPopupStep(superExpression, superTypes))
                    .showInBestPositionFor(editor)
            }
        }
    }

    private fun KtSuperExpression.specifySuperType(superType: String) {
        project.executeWriteCommand("Specify supertype") {
            val label = this.labelQualifier?.text ?: ""
            replace(KtPsiFactory(this).createExpression("super<$superType>$label"))
        }
    }

    private fun createListPopupStep(superExpression: KtSuperExpression, superTypes: List<String>): ListPopupStep<*> {
        return object : BaseListPopupStep<String>("Choose supertype", superTypes) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    superExpression.specifySuperType(selectedValue)
                }
                return PopupStep.FINAL_CHOICE
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtSuperExpression>? {
            val superExpression = diagnostic.psiElement as? KtSuperExpression ?: return null
            val qualifiedExpression = superExpression.getQualifiedExpressionForReceiver() ?: return null
            val selectorExpression = qualifiedExpression.selectorExpression ?: return null

            val containingClassOrObject = superExpression.getStrictParentOfType<KtClassOrObject>() ?: return null
            val superTypeListEntries = containingClassOrObject.superTypeListEntries
            if (superTypeListEntries.isEmpty()) return null

            val context = superExpression.analyze(BodyResolveMode.PARTIAL)
            val superTypes = superTypeListEntries.mapNotNull {
                val typeReference = it.typeReference ?: return@mapNotNull null
                val typeElement = it.typeReference?.typeElement ?: return@mapNotNull null
                val kotlinType = context[BindingContext.TYPE, typeReference] ?: return@mapNotNull null
                typeElement to kotlinType
            }
            if (superTypes.size != superTypeListEntries.size) return null

            val psiFactory = KtPsiFactory(superExpression)
            val superTypesForSuperExpression = superTypes.mapNotNull { (typeElement, kotlinType) ->
                if (superTypes.any { it.second != kotlinType && it.second.isSubtypeOf(kotlinType) }) return@mapNotNull null
                val fqName = kotlinType.fqName ?: return@mapNotNull null
                val fqNameAsString = fqName.asString()
                val name = if (typeElement.text.startsWith(fqNameAsString)) fqNameAsString else fqName.shortName().asString()
                val newQualifiedExpression = psiFactory.createExpressionByPattern("super<$name>.$0", selectorExpression)
                val newContext = newQualifiedExpression.analyzeAsReplacement(qualifiedExpression, context)
                if (newQualifiedExpression.getResolvedCall(newContext)?.resultingDescriptor == null) return@mapNotNull null
                if (newContext.diagnostics.noSuppression().forElement(newQualifiedExpression).isNotEmpty()) return@mapNotNull null
                name
            }
            if (superTypesForSuperExpression.isEmpty()) return null

            return SpecifySuperTypeFix(superExpression, superTypesForSuperExpression)
        }
    }
}
