// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.notification.Notifications
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.impl.stores.SaveSessionAndFile
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectManagerImpl.UnableToSaveProjectNotification
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import com.intellij.util.containers.mapSmart
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
open class ProjectSaveSessionProducerManager(private val project: Project) : SaveSessionProducerManager() {
  suspend fun saveWithAdditionalSaveSessions(extraSessions: List<SaveSession>): SaveResult {
    val saveSessions = SmartList<SaveSession>()
    collectSaveSessions(saveSessions)
    if (saveSessions.isEmpty() && extraSessions.isEmpty()) {
      return SaveResult.EMPTY
    }

    val saveResult = withEdtContext(project) {
      runWriteAction {
        val r = SaveResult()
        saveSessions(extraSessions, r)
        saveSessions(saveSessions, r)
        r
      }
    }
    validate(saveResult)
    return saveResult
  }

  private suspend fun validate(saveResult: SaveResult) {
    val notifications = getUnableToSaveNotifications()
    val readonlyFiles = saveResult.readonlyFiles
    if (readonlyFiles.isEmpty()) {
      notifications.forEach { it.expire() }
      return
    }

    if (notifications.isNotEmpty()) {
      throw UnresolvedReadOnlyFilesException(readonlyFiles.mapSmart { it.file })
    }

    val status = ensureFilesWritable(project, getFilesList(readonlyFiles))
    if (status.hasReadonlyFiles()) {
      val unresolvedReadOnlyFiles = status.readonlyFiles.toList()
      dropUnableToSaveProjectNotification(project, unresolvedReadOnlyFiles)
      saveResult.addError(UnresolvedReadOnlyFilesException(unresolvedReadOnlyFiles))
      return
    }

    val oldList = readonlyFiles.toTypedArray()
    readonlyFiles.clear()
    withEdtContext(project) {
      runWriteAction {
        val r = SaveResult()
        for (entry in oldList) {
          executeSave(entry.session, r)
        }
        r
      }
    }.appendTo(saveResult)

    if (readonlyFiles.isNotEmpty()) {
      dropUnableToSaveProjectNotification(project, getFilesList(readonlyFiles))
      saveResult.addError(UnresolvedReadOnlyFilesException(readonlyFiles.mapSmart { it.file }))
    }
  }

  private fun dropUnableToSaveProjectNotification(project: Project, readOnlyFiles: List<VirtualFile>) {
    val notifications = getUnableToSaveNotifications()
    if (notifications.isEmpty()) {
      Notifications.Bus.notify(UnableToSaveProjectNotification(project, readOnlyFiles), project)
    }
    else {
      notifications[0].setFiles(readOnlyFiles)
    }
  }

  private fun getUnableToSaveNotifications(): Array<out UnableToSaveProjectNotification> {
    val notificationManager = serviceIfCreated<NotificationsManager>() ?: return emptyArray()
    return notificationManager.getNotificationsOfType(UnableToSaveProjectNotification::class.java, project)
  }
}

private fun getFilesList(readonlyFiles: List<SaveSessionAndFile>) = readonlyFiles.mapSmart { it.file }