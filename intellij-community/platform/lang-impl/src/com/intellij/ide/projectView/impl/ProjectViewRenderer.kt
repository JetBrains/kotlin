// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.ui.UISettings.Companion.instance
import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.text.JBDateFormat
import com.intellij.util.ui.tree.TreeUtil
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.JTree

open class ProjectViewRenderer : NodeRenderer() {

    init {
        isOpaque = false
        isIconOpaque = false
        isTransparentIconBackground = true
    }

    override fun customizeCellRenderer(tree: JTree,
                                       value: Any?,
                                       selected: Boolean,
                                       expanded: Boolean,
                                       leaf: Boolean,
                                       row: Int,
                                       hasFocus: Boolean) {
        super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus)

        val userObject = TreeUtil.getUserObject(value)
        if (userObject is ProjectViewNode<*> && instance.showInplaceComments) {
            appendInplaceComments(userObject)
        }
    }

    fun appendInplaceComments(project: Project?, file: VirtualFile?) {
        val ioFile =
            if (file == null || file.isDirectory || !file.isInLocalFileSystem) null
            else VfsUtilCore.virtualToIoFile(file)

        val attr =
            try {
                if (ioFile == null) null
                else Files.readAttributes(Paths.get(ioFile.toURI()), BasicFileAttributes::class.java)
            } catch (ex: Exception) {
                null
            }

        if (attr != null) {
            append("  ")
            val attributes = SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES
            append(JBDateFormat.getFormatter().formatDateTime(attr.lastModifiedTime().toMillis()), attributes)
            append(", " + StringUtil.formatFileSize(attr.size()), attributes)
        }

        if (Registry.`is`("show.last.visited.timestamps") && file != null && project != null) {
            IdeDocumentHistoryImpl.appendTimestamp(project, this, file)
        }
    }

    fun appendInplaceComments(node: ProjectViewNode<*>) {
        val parentNode = node.parent
        val content = node.value
        if (content is PsiFileSystemItem || content !is PsiElement || parentNode != null && parentNode.value is PsiDirectory) {
            appendInplaceComments(node.project, node.virtualFile)
        }
    }
}