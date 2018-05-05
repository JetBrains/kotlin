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

package org.jetbrains.android.inspections.klint

import com.android.SdkConstants.FQCN_SUPPRESS_LINT
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.android.util.AndroidBundle
import org.jetbrains.kotlin.android.hasBackingField
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import javax.swing.Icon


class SuppressLintIntentionAction(val id: String, val element: PsiElement) : IntentionAction, Iconable {

    private companion object {
        val INTENTION_NAME_PREFIX = "AndroidKLint"
        val SUPPRESS_LINT_MESSAGE = "android.lint.fix.suppress.lint.api.annotation"
        val FQNAME_SUPPRESS_LINT = FqName(FQCN_SUPPRESS_LINT)
    }

    private val lintId = getLintId(id)

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

    override fun getText(): String = AndroidBundle.message(SUPPRESS_LINT_MESSAGE, lintId)

    override fun getFamilyName() = text

    override fun getIcon(flags: Int): Icon? = AllIcons.Actions.Cancel

    override fun startInWriteAction() = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file !is KtFile) {
            return
        }

        val annotationContainer = PsiTreeUtil.findFirstParent(element, true) { it.isSuppressLintTarget() } ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(annotationContainer)) {
            return
        }

        val argument = "\"$lintId\""

        when (annotationContainer) {
            is KtModifierListOwner -> annotationContainer.addAnnotation(
                    FQNAME_SUPPRESS_LINT,
                    argument,
                    whiteSpaceText = if (annotationContainer.isNewLineNeededForAnnotation()) "\n" else " ",
                    addToExistingAnnotation = { entry -> addArgumentToAnnotation(entry, argument) })
        }
    }

    private fun addArgumentToAnnotation(entry: KtAnnotationEntry, argument: String): Boolean {
        // add new arguments to an existing entry
        val args = entry.valueArgumentList
        val psiFactory = KtPsiFactory(entry)
        val newArgList = psiFactory.createCallArguments("($argument)")
        when {
            args == null -> // new argument list
                entry.addAfter(newArgList, entry.lastChild)
            args.arguments.isEmpty() -> // replace '()' with a new argument list
                args.replace(newArgList)
            args.arguments.none { it.textMatches(argument) } ->
                args.addArgument(newArgList.arguments[0])
        }

        return true
    }

    private fun getLintId(intentionId: String) =
            if (intentionId.startsWith(INTENTION_NAME_PREFIX)) intentionId.substring(INTENTION_NAME_PREFIX.length) else intentionId

    private fun KtElement.isNewLineNeededForAnnotation(): Boolean {
        return !(this is KtParameter ||
                 this is KtTypeParameter ||
                 this is KtPropertyAccessor)
    }

    private fun PsiElement.isSuppressLintTarget(): Boolean {
        return this is KtDeclaration &&
               (this as? KtProperty)?.hasBackingField() ?: true &&
               this !is KtFunctionLiteral &&
               this !is KtDestructuringDeclaration
    }
}