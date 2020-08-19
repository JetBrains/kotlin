/*
 * Copyright 2010-2020 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.help.HelpManager
import com.intellij.psi.PsiReference
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtWhenExpression

class KotlinInlinePropertyDialog(
    property: KtProperty,
    reference: PsiReference?,
    private val assignmentToDelete: KtBinaryExpression?,
    withPreview: Boolean = true,
    editor: Editor?,
) : AbstractKotlinInlineDialog<KtProperty>(property, reference, editor) {
    private val simpleLocal = declaration.isLocal && (reference == null || occurrencesNumber == 1)

    init {
        setPreviewResults(withPreview && shouldBeShown())
        if (simpleLocal) {
            setDoNotAskOption(object : DoNotAskOption {
                override fun isToBeShown() = EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog

                override fun setToBeShown(value: Boolean, exitCode: Int) {
                    EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog = value
                }

                override fun canBeHidden() = true

                override fun shouldSaveOptionsOnCancel() = false

                override fun getDoNotShowMessage() = KotlinBundle.message("message.do.not.show.for.local.variables.in.future")
            })
        }
        init()
    }

    fun shouldBeShown() = !simpleLocal || EditorSettingsExternalizable.getInstance().isShowInlineLocalDialog

    override fun doHelpAction() = HelpManager.getInstance().invokeHelp(HelpID.INLINE_VARIABLE)

    override fun isInlineThis() = KotlinRefactoringSettings.instance.INLINE_LOCAL_THIS

    public override fun doAction() {
        invokeRefactoring(
            KotlinInlinePropertyProcessor(
                declaration = declaration,
                reference = reference,
                inlineThisOnly = isInlineThisOnly,
                deleteAfter = !isInlineThisOnly && !isKeepTheDeclaration,
                isWhenSubjectVariable = (declaration.parent as? KtWhenExpression)?.subjectVariable == declaration,
                editor = editor,
                statementToDelete = assignmentToDelete,
            )
        )

        val settings = KotlinRefactoringSettings.instance
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_LOCAL_THIS = isInlineThisOnly
        }
    }
}
