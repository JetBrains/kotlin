/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElementFinder
import org.jetbrains.kotlin.idea.core.script.KotlinScriptDependenciesClassFinder
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesModificationTracker
import org.jetbrains.kotlin.idea.core.script.configuration.CompositeScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.ScriptingSupportHelper
import org.jetbrains.kotlin.idea.core.script.debug
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
    private val concurrentUpdates = AtomicInteger()

    @Synchronized
    @Suppress("UNUSED_PARAMETER")
    fun invalidate(file: VirtualFile) {
        // todo: record invalided files for some optimisations in update
        invalidate()
    }

    @Synchronized
    fun invalidate() {
        checkInTransaction()
        invalidated = true
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

    private fun scheduleUpdateIfInvalid() {
        if (!invalidated) return
        invalidated = false

        ensureUpdateScheduled()
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

    /**
     * Used from legacy FS cache only, don't use
     */
    @Synchronized
    fun updateSynchronously() {
        syncLock.withLock {
            scheduledUpdate?.cancel()
            doUpdate(false)
        }
    }

    fun doUpdate(underProgressManager: Boolean = true) {
        syncLock.withLock {
            val updates = manager.collectRootsAndCheckNew()

            if (!updates.changed) return

            try {
                ProgressManager.checkCanceled()

                if (updates.hasNewRoots) {
                    notifyRootsChanged()
                }

                PsiElementFinder.EP.findExtensionOrFail(KotlinScriptDependenciesClassFinder::class.java, project)
                    .clearCache()

                ScriptDependenciesModificationTracker.getInstance(project).incModificationCount()

                if (updates.updatedScripts.isNotEmpty()) {
                    ScriptingSupportHelper.updateHighlighting(project) {
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
}
