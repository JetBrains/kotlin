package com.intellij.codeInsight.hint

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.extensions.ExtensionPointName

interface ImplementationViewDocumentFactory{
    fun createDocument(element: ImplementationViewElement) : Document?
    @JvmDefault fun tuneEditorBeforeShow(editor: EditorEx) = Unit
    @JvmDefault fun tuneEditorAfterShow(editor: EditorEx) = Unit

    companion object {
        @JvmField val EP_NAME = ExtensionPointName.create<ImplementationViewDocumentFactory>("com.intellij.implementationViewDocumentFactory")
    }
}
