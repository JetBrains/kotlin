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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.LookupEvent
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.util.application.getServiceSafe

class LookupCancelService {
    internal class Reminiscence(editor: Editor, offset: Int) {
        var editor: Editor? = editor
        private var marker: RangeMarker? = editor.document.createRangeMarker(offset, offset)

        // forget about auto-popup cancellation when the caret is moved to the start or before it
        private var editorListener: CaretListener? = object : CaretListener {
            override fun caretPositionChanged(e: CaretEvent) {
                if (!marker!!.isValid || editor.logicalPositionToOffset(e.newPosition) <= offset) {
                    dispose()
                }
            }
        }

        init {
            ApplicationManager.getApplication()!!.assertIsDispatchThread()
            editor.caretModel.addCaretListener(editorListener!!)
        }

        fun matches(editor: Editor, offset: Int): Boolean {
            return editor == this.editor && marker?.startOffset == offset
        }

        fun dispose() {
            ApplicationManager.getApplication()!!.assertIsDispatchThread()
            if (marker != null) {
                editor!!.caretModel.removeCaretListener(editorListener!!)
                marker = null
                editor = null
                editorListener = null
            }
        }
    }

    internal val lookupCancelListener = object : LookupListener {
        override fun lookupCanceled(event: LookupEvent) {
            val lookup = event.lookup
            if (event.isCanceledExplicitly && lookup.isCompletion) {
                val offset = lookup.currentItem?.getUserData(LookupCancelService.AUTO_POPUP_AT)
                if (offset != null) {
                    lastReminiscence?.dispose()
                    if (offset <= lookup.editor.document.textLength) {
                        lastReminiscence = Reminiscence(lookup.editor, offset)
                    }
                }
            }
        }
    }

    internal fun disposeLastReminiscence(editor: Editor) {
        if (lastReminiscence?.editor == editor) {
            lastReminiscence!!.dispose()
            lastReminiscence = null
        }
    }

    private var lastReminiscence: Reminiscence? = null

    companion object {
        fun getInstance(project: Project): LookupCancelService = project.getServiceSafe()

        fun getServiceIfCreated(project: Project): LookupCancelService? = project.getServiceIfCreated(LookupCancelService::class.java)

        val AUTO_POPUP_AT = Key<Int>("LookupCancelService.AUTO_POPUP_AT")
    }

    fun wasAutoPopupRecentlyCancelled(editor: Editor, offset: Int): Boolean {
        return lastReminiscence?.matches(editor, offset) ?: false
    }

}
