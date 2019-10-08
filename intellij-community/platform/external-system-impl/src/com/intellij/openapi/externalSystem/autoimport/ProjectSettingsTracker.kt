// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.LocalTimeCounter.currentTime
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.ApiStatus
import java.io.File

@ApiStatus.Internal
abstract class ProjectSettingsTracker(
  private val projectTracker: ProjectTracker,
  private val projectAware: ExternalSystemProjectAware,
  private val parentDisposable: Disposable
) {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private val status = ProjectStatus(debugName = "${this::class.java.simpleName} (${projectAware.projectId.readableName})")

  @Volatile
  private var settingsFilesCRC: Map<String, Long> = emptyMap()

  protected val cachedSettingsFilesCRC get() = settingsFilesCRC

  protected abstract fun calculateSettingsFilesCRC(): Map<String, Long>

  fun isUpToDate() = status.isUpToDate()

  private fun hasChanges(): Boolean {
    val oldSettingsFilesCRC = settingsFilesCRC
    val newSettingsFilesCRC = calculateSettingsFilesCRC()
    if (newSettingsFilesCRC.size != oldSettingsFilesCRC.size) return true
    return newSettingsFilesCRC.any { it.value != oldSettingsFilesCRC[it.key] }
  }

  /**
   * Usually all crc hashes must be previously calculated
   *  => this apply will be fast
   *  => collisions is a rare thing
   */
  fun applyChanges() {
    submitSettingsFilesRefresh {
      submitNonBlockingReadAction {
        settingsFilesCRC = calculateSettingsFilesCRC()
        status.markSynchronized(currentTime())
        projectTracker.scheduleProjectNotificationUpdate()
      }
    }
  }

  fun refreshChanges() {
    submitSettingsFilesRefresh {
      submitNonBlockingReadAction {
        when (hasChanges()) {
          true -> status.markDirty(currentTime())
          else -> status.markReverted(currentTime())
        }
        projectTracker.scheduleProjectRefresh()
      }
    }
  }


  fun getState() = State(settingsFilesCRC.toMap())

  fun loadState(state: State) {
    settingsFilesCRC = state.settingsFiles.toMap()
  }

  private fun isAsyncAllowed() = !ApplicationManager.getApplication().isUnitTestMode

  private fun submitSettingsFilesRefresh(callback: () -> Unit = {}) {
    invokeLater {
      val fileDocumentManager = FileDocumentManager.getInstance()
      fileDocumentManager.saveAllDocuments()
      val localFileSystem = LocalFileSystem.getInstance()
      val settingsFiles = projectAware.settingsFiles.map { File(it) }
      localFileSystem.refreshIoFiles(settingsFiles, isAsyncAllowed(), false, callback)
    }
  }

  private fun submitNonBlockingReadAction(action: () -> Unit) {
    if (!isAsyncAllowed()) {
      action()
      return
    }
    ReadAction.nonBlocking(action)
      .expireWith(parentDisposable)
      .submit(AppExecutorUtil.getAppExecutorService())
  }

  private fun invokeLater(action: () -> Unit) {
    val transactionGuard = TransactionGuard.getInstance()
    if (!isAsyncAllowed()) {
      transactionGuard.submitTransactionAndWait(Runnable { action() })
    }
    else {
      transactionGuard.submitTransactionLater(parentDisposable, Runnable { action() })
    }
  }

  data class State(var settingsFiles: Map<String, Long> = emptyMap())

  protected inner class ProjectSettingsListener {
    @Volatile
    private var hasRelevantChanges = false

    @Volatile
    private var settingsFilesSnapshot: Set<String> = emptySet()

    fun isRelevant(path: String): Boolean {
      val isRelevant = path in settingsFilesSnapshot
      hasRelevantChanges = hasRelevantChanges || isRelevant
      return isRelevant
    }

    fun updateFile(path: String, modificationStamp: Long) {
      hasRelevantChanges = true
      logModificationAsDebug(path, modificationStamp)
      status.markModified(currentTime())
    }

    fun init() {
      hasRelevantChanges = false
      settingsFilesSnapshot = projectAware.settingsFiles + cachedSettingsFilesCRC.keys
    }

    fun apply() {
      if (hasRelevantChanges) {
        submitNonBlockingReadAction {
          if (!hasChanges()) status.markReverted(currentTime())
          projectTracker.scheduleChangeProcessing()
        }
      }
    }

    private fun logModificationAsDebug(path: String, modificationStamp: Long) {
      if (LOG.isDebugEnabled) {
        val projectPath = projectAware.projectId.externalProjectPath
        val relativePath = FileUtil.getRelativePath(projectPath, path, '/') ?: path
        LOG.debug("File $relativePath is modified at ${modificationStamp}")
      }
    }
  }
}