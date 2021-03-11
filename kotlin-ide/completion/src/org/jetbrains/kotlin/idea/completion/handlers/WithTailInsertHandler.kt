/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.completion.KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.tryGetOffset

class WithTailInsertHandler(
    val tailText: String,
    val spaceBefore: Boolean,
    val spaceAfter: Boolean,
    val overwriteText: Boolean = true
) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        item.handleInsert(context)
        postHandleInsert(context, item)
    }

    fun postHandleInsert(context: InsertionContext, item: LookupElement) {
        val completionChar = context.completionChar
        if (completionChar == tailText.singleOrNull() || (spaceAfter && completionChar == ' ')) {
            context.setAddCompletionChar(false)
        }
        //TODO: what if completion char is different?

        val document = context.document
        PsiDocumentManager.getInstance(context.project).doPostponedOperationsAndUnblockDocument(document)

        var tailOffset = context.tailOffset
        if (completionChar == Lookup.REPLACE_SELECT_CHAR && item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) != null) {
            context.offsetMap.tryGetOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET)
                ?.let { tailOffset = it }
        }

        val moveCaret = context.editor.caretModel.offset == tailOffset

        //TODO: analyze parenthesis balance to decide whether to replace or not
        var insert = true
        if (overwriteText) {
            var offset = tailOffset
            if (tailText != " ") {
                offset = document.charsSequence.skipSpacesAndLineBreaks(offset)
            }
            if (shouldOverwriteChar(document, offset)) {
                insert = false
                offset += tailText.length
                tailOffset = offset

                if (spaceAfter && document.charsSequence.isCharAt(offset, ' ')) {
                    document.deleteString(offset, offset + 1)
                }
            }
        }

        var textToInsert = ""
        if (insert) {
            textToInsert = tailText
            if (spaceBefore) textToInsert = " " + textToInsert
        }
        if (spaceAfter) textToInsert += " "

        document.insertString(tailOffset, textToInsert)

        if (moveCaret) {
            context.editor.caretModel.moveToOffset(tailOffset + textToInsert.length)

            if (tailText == ",") {
                AutoPopupController.getInstance(context.project)?.autoPopupParameterInfo(context.editor, null)
            }
        }
    }

    private fun shouldOverwriteChar(document: Document, offset: Int): Boolean {
        if (!document.isTextAt(offset, tailText)) return false
        if (tailText == " " && document.charsSequence.isCharAt(offset + 1, '}')) return false // do not overwrite last space before '}'
        return true
    }

    companion object {
        val COMMA = WithTailInsertHandler(",", spaceBefore = false, spaceAfter = true /*TODO: use code style option*/)
        val RPARENTH = WithTailInsertHandler(")", spaceBefore = false, spaceAfter = false)
        val RBRACKET = WithTailInsertHandler("]", spaceBefore = false, spaceAfter = false)
        val RBRACE = WithTailInsertHandler("}", spaceBefore = true, spaceAfter = false)
        val ELSE = WithTailInsertHandler("else", spaceBefore = true, spaceAfter = true)
        val EQ = WithTailInsertHandler("=", spaceBefore = true, spaceAfter = true) /*TODO: use code style options*/
        val SPACE = WithTailInsertHandler(" ", spaceBefore = false, spaceAfter = false, overwriteText = true)
    }
}
