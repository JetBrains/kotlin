/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

class KtFileTreeNode(
    project: Project?,
    val ktFile: KtFile,
    viewSettings: ViewSettings
) : PsiFileNode(project, ktFile, viewSettings) {

    override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
        if (!settings.isShowMembers) return emptyList()

        val declarations = (if (ktFile.isScript()) ktFile.script else ktFile)
            ?.declarations
            ?: return emptyList()

        return declarations.map {
            if (it is KtClassOrObject)
                KtClassOrObjectTreeNode(project, it, settings)
            else
                KtDeclarationTreeNode(project, it, settings)
        }
    }
}