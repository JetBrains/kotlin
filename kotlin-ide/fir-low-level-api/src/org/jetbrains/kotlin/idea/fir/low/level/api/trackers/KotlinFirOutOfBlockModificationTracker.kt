/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trackers

import com.intellij.ProjectTopics
import com.intellij.lang.ASTNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.pom.PomManager
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingInBodyDeclarationWith
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileElementFactory
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import java.util.*

internal class KotlinFirModificationTrackerService(project: Project) : Disposable {
    init {
        val model = PomManager.getModel(project)
        model.addModelListener(Listener())

        val connection = project.messageBus.connect(this)
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                projectGlobalOutOfBlockInKotlinFilesModificationCount++

                // todo increase modificationCountForModule
            }
        })
    }

    internal var projectGlobalOutOfBlockInKotlinFilesModificationCount = 0L
        private set

    internal fun getOutOfBlockModificationCountForModules(module: Module): Long =
        modificationCountForModule[module] ?: 0L

    private val modificationCountForModule = WeakHashMap<Module, Long>()
    private val treeAspect = TreeAspect.getInstance(project)

    override fun dispose() {}

    private inner class Listener : PomModelListener {
        override fun modelChanged(event: PomModelEvent) {
            val changeSet = event.getChangeSet(treeAspect) as TreeChangeEvent? ?: return
            if (changeSet.rootElement.psi.language != KotlinLanguage.INSTANCE) return
            val changedElements = changeSet.changedElements

            var isOutOfBlockChangeInAnyModule = false

            changedElements.forEach { element ->
                val isOutOfBlock = element.isOutOfBlockChange(changeSet)
                isOutOfBlockChangeInAnyModule = isOutOfBlockChangeInAnyModule || isOutOfBlock
                if (isOutOfBlock) {
                    element.psi.module?.let { module ->
                        modificationCountForModule.compute(module) { _, value -> (value ?: 0) + 1 }
                    }
                }
            }

            if (isOutOfBlockChangeInAnyModule) {
                projectGlobalOutOfBlockInKotlinFilesModificationCount++
            }
        }

        private fun ASTNode.isOutOfBlockChange(changeSet: TreeChangeEvent): Boolean {
            val nodes = changeSet.getChangesByElement(this).affectedChildren
            return nodes.any { node ->
                val psi = node.psi ?: return@any true
                val container = psi.getNonLocalContainingInBodyDeclarationWith() ?: return@any true
                !FileElementFactory.isReanalyzableContainer(container)
            }
        }

        override fun isAspectChangeInteresting(aspect: PomModelAspect): Boolean =
            treeAspect == aspect
    }
}