// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.autoimport.AsyncFileChangeListenerBase
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus
import com.intellij.openapi.externalSystem.util.calculateCrc
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.LocalTimeCounter.currentTime
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.concurrency.Semaphore
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.TestOnly
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ProjectSettingsTracker(
  private val projectTracker: ProjectTracker,
  private val projectStatus: ProjectStatus,
  private val projectAware: ExternalSystemProjectAware,
  private val parentDisposable: Disposable
) {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private var settingsFilesCRC: Map<String, Long> = LinkedHashMap()

  fun hasChanges(): Boolean {
    val oldSettingsFilesCRC = settingsFilesCRC
    val newSettingsFilesCRC = getSettingsFilesCRC()
    if (newSettingsFilesCRC.size != oldSettingsFilesCRC.size) return true
    return newSettingsFilesCRC.any { it.value != oldSettingsFilesCRC[it.key] }
  }

  /**
   * Usually all crc hashes must be previously calculated
   *  => this apply will be fast
   *  => collisions is a rare thing
   */
  fun applyChanges() {
    afterSettingsFilesRefresh {
      runNonBlocking {
        settingsFilesCRC = getSettingsFilesCRC()
      }
    }
  }

  fun getState() = State(settingsFilesCRC.toMap())

  fun loadState(state: State) {
    settingsFilesCRC = state.settingsFiles.toMap()
    afterSettingsFilesRefresh {
      runNonBlocking {
        when (hasChanges()) {
          true -> projectStatus.markDirty(currentTime())
          else -> projectStatus.markReverted(currentTime())
        }
        projectTracker.scheduleProjectRefresh()
      }
    }
  }

  private fun afterSettingsFilesRefresh(action: () -> Unit) {
    val localFileSystem = LocalFileSystem.getInstance()
    val settingsFiles = projectAware.settingsFiles.map { File(it) }
    localFileSystem.refreshIoFiles(settingsFiles, true, false, makeAsyncAction(action))
  }

  private fun getSettingsFilesCRC(): Map<String, Long> {
    val localFileSystem = LocalFileSystem.getInstance()
    return projectAware.settingsFiles
      .mapNotNull { localFileSystem.findFileByPath(it) }
      .map { it.path to it.calculateCrc() }
      .toMap()
  }

  private fun runNonBlocking(action: () -> Unit) {
    ReadAction.nonBlocking(makeAsyncAction(action))
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

  init {
    val settingsListener = ProjectSettingsListener()
    val fileManager = VirtualFileManager.getInstance()
    fileManager.addAsyncFileListener(settingsListener, parentDisposable)
  }

  data class State(var settingsFiles: Map<String, Long> = emptyMap())

  private inner class ProjectSettingsListener : AsyncFileChangeListenerBase() {
    private var hasRelevantChanges: Boolean = false
    private var settingsFilesSnapshot: Set<String> = emptySet()

    override fun isRelevant(path: String) = path in settingsFilesSnapshot

    override fun updateFile(file: VirtualFile) {
      hasRelevantChanges = true
      logModificationAsDebug(file)
      projectStatus.markModified(currentTime())
    }

    override fun init() {
      hasRelevantChanges = false
      settingsFilesSnapshot = projectAware.settingsFiles + settingsFilesCRC.keys
    }

    override fun apply() {
      if (hasRelevantChanges) {
        runNonBlocking {
          if (!hasChanges()) projectStatus.markReverted(currentTime())
          projectTracker.scheduleProjectNotificationUpdate()
        }
      }
    }

    private fun logModificationAsDebug(file: VirtualFile) {
      if (LOG.isDebugEnabled) {
        val projectPath = projectAware.projectId.externalProjectPath
        val relativePath = FileUtil.getRelativePath(projectPath, file.path, '/') ?: file.path
        LOG.debug("File $relativePath is modified at ${file.modificationStamp}")
      }
    }
  }
}