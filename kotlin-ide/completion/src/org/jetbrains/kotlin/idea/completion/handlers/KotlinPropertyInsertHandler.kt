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

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.util.CallType

class KotlinPropertyInsertHandler(callType: CallType<*>) : KotlinCallableInsertHandler(callType) {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val surroundedWithBraces = surroundWithBracesIfInStringTemplate(context)

        super.handleInsert(context, item)

        if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
            deleteEmptyParenthesis(context)
        }

        if (surroundedWithBraces) {
            removeRedundantBracesInStringTemplate(context)
        }
    }

    private fun deleteEmptyParenthesis(context: InsertionContext) {
        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()
        psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)

        val offset = context.tailOffset
        val document = context.document
        val chars = document.charsSequence

        val lParenOffset = chars.indexOfSkippingSpace('(', offset) ?: return
        val rParenOffset = chars.indexOfSkippingSpace(')', lParenOffset + 1) ?: return

        document.deleteString(offset, rParenOffset + 1)
    }
}