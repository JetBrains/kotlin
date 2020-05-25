/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.ucache

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Disposer
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
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Holder for [ScriptClassRootsCache].
 *
 * Updates of [classpathRoots] performed asynchronously using the copy-on-write strategy.
 * [gatherRoots] called when updating is required. Cache will built from the scratch.
 *
 * Updates can be coalesced by using `update { invalidate() }` transaction.
 * As an alternative you can just call [invalidateAndCommit].
 *
 * After update roots changed event will be triggered if there are new root.
 * This will start indexing.
 * Also analysis cache will be cleared and changed opened script files will be reanalyzed.
 */
class ScriptClassRootsUpdater(
    val project: Project,
    val manager: CompositeScriptConfigurationManager,
    private val gatherRoots: (ScriptClassRootsBuilder) -> Unit
) {
    private var lastSeen: ScriptClassRootsCache? = null
    private var invalidated: Boolean = false
    private var syncUpdateRequired: Boolean = false
    private val concurrentUpdates = AtomicInteger()

    private fun recreateRootsCache(): ScriptClassRootsCache {
        val builder = ScriptClassRootsBuilder(project)
        gatherRoots(builder)
        return builder.build()
    }

    @Volatile
    var classpathRoots: ScriptClassRootsCache = recreateRootsCache()
        private set

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

    fun invalidateAndCommit() {
        update { invalidate() }
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
    private fun ensureUpdateScheduled() {
        scheduledUpdate?.cancel()
        runReadAction {
            if (project.isDisposed && !Disposer.isDisposing(project)) {
                scheduledUpdate = BackgroundTaskUtil.executeOnPooledThread(project) {
                    doUpdate()
                }
            }
        }
    }

    @Synchronized
    private fun updateSynchronously() {
        scheduledUpdate?.cancel()
        doUpdate(false)
    }

    internal fun checkInvalidSdks(remove: Sdk? = null) {
        // sdks should be updated synchronously to avoid disposed roots usage
        syncLock.withLock {
            val current = classpathRoots
            val actualSdks = current.sdks.rebuild(remove = remove)
            if (actualSdks != current.sdks) {
                // don't call invalidateAndCommit as it may be synchronous
                // let's update sdks immediately and schedule cache rebuilding
                classpathRoots = current.withUpdatedSdks(actualSdks)
                ensureUpdateScheduled()
            }
        }
    }

    private fun doUpdate(underProgressManager: Boolean = true) {
        syncLock.withLock {
            try {
                val updates = recreateRootsCacheAndDiff()

                if (!updates.changed) return

                if (underProgressManager) {
                    ProgressManager.checkCanceled()
                }

                if (project.isDisposed) return

                if (updates.hasNewRoots) {
                    notifyRootsChanged()
                }

                PsiElementFinder.EP.findExtensionOrFail(KotlinScriptDependenciesClassFinder::class.java, project)
                    .clearCache()

                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

                if (updates.hasUpdatedScripts) {
                    updateHighlighting(project) {
                        updates.isScriptChanged(it.path)
                    }
                }

                lastSeen = updates.cache
            } finally {
                synchronized(this) {
                    scheduledUpdate = null
                }
            }
        }
    }

    private fun recreateRootsCacheAndDiff(): ScriptClassRootsCache.Updates {
        val new = recreateRootsCache()
        classpathRoots = new
        return new.diff(lastSeen)
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

    private fun updateHighlighting(project: Project, filter: (VirtualFile) -> Boolean) {
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