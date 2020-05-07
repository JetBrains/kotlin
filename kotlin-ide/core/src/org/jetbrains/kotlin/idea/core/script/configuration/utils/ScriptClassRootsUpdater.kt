/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.idea.core.script.KotlinScriptDependenciesClassFinder
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Utility for postponing indexing of new roots to the end of some bulk operation.
 */
class ScriptClassRootsUpdater(
    val project: Project,
    val manager: CompositeScriptConfigurationManager
) {
    private var invalidated: Boolean = false
    private var syncUpdateRequired: Boolean = false
    private val concurrentUpdates = AtomicInteger()

    /**
     * @param synchronous Used from legacy FS cache only, don't use
     */
    @Synchronized
    @Suppress("UNUSED_PARAMETER")
    fun invalidate(file: VirtualFile, synchronous: Boolean = false) {
        // todo: record invalided files for some optimisations in update
        invalidate(synchronous)
    }

    /**
     * @param synchronous Used from legacy FS cache only, don't use
     */
    @Synchronized
    fun invalidate(synchronous: Boolean = false) {
        checkInTransaction()
        invalidated = true
        if (synchronous) {
            syncUpdateRequired = true
        }
    }

    fun checkInTransaction() {
        check(concurrentUpdates.get() > 0)
    }

    inline fun <T> update(body: () -> T): T {
        beginUpdating()
        return try {
            body()
        } finally {
            commit()
        }
    }

    fun beginUpdating() {
        concurrentUpdates.incrementAndGet()
    }

    fun commit() {
        concurrentUpdates.decrementAndGet()

        // run update even in inner transaction
        // (outer transaction may be async, so it would be better to not wait it)
        scheduleUpdateIfInvalid()
    }

    @Synchronized
    private fun scheduleUpdateIfInvalid() {
        if (!invalidated) return
        invalidated = false

        if (syncUpdateRequired || ApplicationManager.getApplication().isUnitTestMode) {
            syncUpdateRequired = false
            updateSynchronously()
        } else {
            ensureUpdateScheduled()
        }
    }

    private val syncLock = ReentrantLock()
    private var scheduledUpdate: ProgressIndicator? = null

    @Synchronized
    fun ensureUpdateScheduled() {
        scheduledUpdate?.cancel()
        scheduledUpdate = BackgroundTaskUtil.executeOnPooledThread(project) {
            doUpdate()
        }
    }

    @Synchronized
    private fun updateSynchronously() {
        scheduledUpdate?.cancel()
        doUpdate(false)
    }

    private fun doUpdate(underProgressManager: Boolean = true) {
        syncLock.withLock {
            try {
                val updates = manager.collectRootsAndCheckNew()

                if (!updates.changed) return

                if (underProgressManager) {
                    ProgressManager.checkCanceled()
                }

                if (updates.hasNewRoots) {
                    notifyRootsChanged()
                }

                PsiElementFinder.EP.findExtensionOrFail(KotlinScriptDependenciesClassFinder::class.java, project)
                    .clearCache()

                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

                if (updates.updatedScripts.isNotEmpty()) {
                    updateHighlighting(project) {
                        it.path in updates.updatedScripts
                    }
                }
            } catch (cancel: ProcessCanceledException) {
                if (underProgressManager) throw cancel
            } finally {
                synchronized(this) {
                    scheduledUpdate = null
                }
            }
        }
    }

    private fun notifyRootsChanged() {
        val doNotifyRootsChanged = Runnable {
            runWriteAction {
                if (project.isDisposed) return@runWriteAction

                debug { "roots change event" }

                ProjectRootManagerEx.getInstanceEx(project)?.makeRootsChange(EmptyRunnable.getInstance(), false, true)
                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()
            }
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            TransactionGuard.submitTransaction(project, doNotifyRootsChanged)
        } else {
            TransactionGuard.getInstance().submitTransactionLater(project, doNotifyRootsChanged)
        }
    }

    fun updateHighlighting(project: Project, filter: (VirtualFile) -> Boolean) {
        if (!project.isOpen) return

        val openFiles = FileEditorManager.getInstance(project).openFiles
        val openedScripts = openFiles.filter { filter(it) }

        if (openedScripts.isEmpty()) return

        GlobalScope.launch(EDT(project)) {
            if (project.isDisposed) return@launch

            openedScripts.forEach {
                PsiManager.getInstance(project).findFile(it)?.let { psiFile ->
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }
}
