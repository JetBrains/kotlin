/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.ui

import com.intellij.ide.util.AbstractTreeClassChooserDialog
import com.intellij.ide.util.gotoByName.GotoFileModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode
import org.jetbrains.kotlin.idea.projectView.KtFileTreeNode
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.psi.KtFile
import javax.swing.tree.DefaultMutableTreeNode

class KotlinFileChooserDialog(
    @NlsContexts.DialogTitle title: String,
    project: Project
) : AbstractTreeClassChooserDialog<KtFile>(
    title,
    project,
    project.projectScope().restrictToKotlinSources(),
    KtFile::class.java,
    null,
    null,
    null,
    false,
    false
) {
    override fun getSelectedFromTreeUserObject(node: DefaultMutableTreeNode): KtFile? = when (val userObject = node.userObject) {
        is KtFileTreeNode -> userObject.ktFile
        is KtClassOrObjectTreeNode -> {
            val containingFile = userObject.value.containingKtFile
            if (containingFile.declarations.size == 1) containingFile else null
        }
        else -> null
    }

    override fun getClassesByName(name: String, checkBoxState: Boolean, pattern: String, searchScope: GlobalSearchScope): List<KtFile> {
        return FilenameIndex.getFilesByName(project, name, searchScope).filterIsInstance<KtFile>()
    }

    override fun createChooseByNameModel() = GotoFileModel(this.project)
}