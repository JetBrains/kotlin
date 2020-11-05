/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trackers

import com.intellij.ProjectTopics
import com.intellij.lang.ASTNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingInBodyDeclarationWith
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.FileElementFactory
import org.jetbrains.kotlin.idea.util.module

internal class KotlinFirModificationTrackerService(project: Project) : Disposable {
    init {
        PomManager.getModel(project).addModelListener(Listener())

        project.messageBus.connect(this).subscribe(
            ProjectTopics.PROJECT_ROOTS,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) = increaseModificationCountForAllModules()
            }
        )
    }

    var projectGlobalOutOfBlockInKotlinFilesModificationCount = 0L
        private set

    private val moduleModificationsState = ModuleModificationsState()

    fun getOutOfBlockModificationCountForModules(module: Module): Long =
        moduleModificationsState.getModificationsCountForModule(module)

    private val treeAspect = TreeAspect.getInstance(project)

    override fun dispose() {}

    private fun increaseModificationCountForAllModules() {
        projectGlobalOutOfBlockInKotlinFilesModificationCount++
        moduleModificationsState.increaseModificationCountForAllModules()
    }

    @TestOnly
    fun incrementModificationsCount() {
        increaseModificationCountForAllModules()
    }

    private inner class Listener : PomModelListener {
        override fun isAspectChangeInteresting(aspect: PomModelAspect): Boolean =
            treeAspect == aspect

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
                        moduleModificationsState.increaseModificationCountForModule(module)
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
    }
}

private class ModuleModificationsState {
    private val modificationCountForModule = hashMapOf<Module, ModuleModifications>()
    private var state: Long = 0L

    fun getModificationsCountForModule(module: Module) = modificationCountForModule.compute(module) { _, modifications ->
        when {
            modifications == null -> ModuleModifications(0, state)
            modifications.state == state -> modifications
            else -> ModuleModifications(modificationsCount = modifications.modificationsCount + 1, state = state)
        }
    }!!.modificationsCount

    fun increaseModificationCountForAllModules() {
        state++
    }

    fun increaseModificationCountForModule(module: Module) {
        modificationCountForModule.compute(module) { _, modifications ->
            when (modifications) {
                null -> ModuleModifications(0, state)
                else -> ModuleModifications(ModuleModifications(0, state).modificationsCount + 1, state)
            }
        }
    }

    private data class ModuleModifications(val modificationsCount: Long, val state: Long)
}