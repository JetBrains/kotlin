/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

open class DialogWithEditor(
    val project: Project,
    title: String,
    val initialText: String
) : DialogWrapper(project, true) {
    val editor: Editor = createEditor()

    init {
        init()
        setTitle(title)
    }

    override final fun init() {
        super.init()
    }

    private fun createEditor(): Editor {
        val editorFactory = EditorFactory.getInstance()!!
        val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, initialText)
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)!!

        val editor = editorFactory.createEditor(document, project, KotlinFileType.INSTANCE, false)
        val settings = editor.settings
        settings.isVirtualSpace = false
        settings.isLineMarkerAreaShown = false
        settings.isFoldingOutlineShown = false
        settings.isRightMarginShown = false
        settings.isAdditionalPageAtBottom = false
        settings.additionalLinesCount = 2
        settings.additionalColumnsCount = 12

        assert(editor is EditorEx)
        (editor as EditorEx).isEmbeddedIntoDialogWrapper = true

        editor.getColorsScheme().setColor(EditorColors.CARET_ROW_COLOR, editor.getColorsScheme().defaultBackground)

        return editor
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(editor.component, BorderLayout.CENTER)
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return editor.contentComponent
    }

    override fun dispose() {
        super.dispose()
        EditorFactory.getInstance()!!.releaseEditor(editor)
    }
}
