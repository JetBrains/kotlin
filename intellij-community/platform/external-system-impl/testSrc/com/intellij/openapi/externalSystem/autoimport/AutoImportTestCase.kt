// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.externalSystem.test.ExternalSystemTestCase
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil

abstract class AutoImportTestCase : ExternalSystemTestCase() {
  override fun getTestsTempDir() = "tmp${System.currentTimeMillis()}"

  override fun getExternalSystemConfigFileName() = throw UnsupportedOperationException()

  protected lateinit var notificationAware: ProjectNotificationAware private set
  protected lateinit var projectTracker: ProjectTracker private set

  override fun setUp() {
    super.setUp()
    notificationAware = ExternalSystemProjectNotificationAware.getInstance(myProject) as ProjectNotificationAware
    projectTracker = ExternalSystemProjectTracker.getInstance(myProject) as ProjectTracker
  }

  protected fun createFile(relativePath: String, content: String? = null): VirtualFile {
    return when (content) {
      null -> createProjectSubFile(relativePath)
      else -> createProjectSubFile(relativePath, content)
    }.also {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }
  }

  protected fun VirtualFile.update(update: Document.() -> Unit) {
    val fileDocumentManager = FileDocumentManager.getInstance()
    val document = fileDocumentManager.getDocument(this)!!
    WriteCommandAction.runWriteCommandAction(myProject) { document.update() }
    fileDocumentManager.saveDocument(document)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }

  protected fun VirtualFile.insertString(offset: Int, string: String) =
    update { insertString(offset, string) }

  protected fun VirtualFile.replaceString(startOffset: Int, endOffset: Int, string: String) =
    update { replaceString(startOffset, endOffset, string) }

  protected fun VirtualFile.delete() {
    WriteCommandAction.runWriteCommandAction(myProject) { delete(null) }
    FileDocumentManager.getInstance().saveAllDocuments()
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
  }

  protected fun assertSimpleState(projectAware: MockProjectAware,
                                  refresh: Int? = null,
                                  subscribe: Int? = null,
                                  unsubscribe: Int? = null,
                                  notified: Boolean,
                                  event: String) {
    assertProjectAware(projectAware, refresh, subscribe, unsubscribe, event)
    assertNotificationAware(notified = notified, event = event)
  }

  protected fun assertProjectAware(projectAware: MockProjectAware,
                                   refresh: Int? = null,
                                   subscribe: Int? = null,
                                   unsubscribe: Int? = null,
                                   event: String) {
    if (refresh != null) assertCountEvent(refresh, projectAware.refreshCounter.get(), "project refresh", event)
    if (subscribe != null) assertCountEvent(subscribe, projectAware.subscribeCounter.get(), "subscribe", event)
    if (unsubscribe != null) assertCountEvent(unsubscribe, projectAware.unsubscribeCounter.get(), "unsubscribe", event)
  }

  private fun assertCountEvent(expected: Int, actual: Int, countEvent: String, event: String) {
    val message = when {
      actual > expected -> "Unexpected $countEvent event"
      actual < expected -> "Expected $countEvent event"
      else -> ""
    }
    assertEquals("$message on $event", expected, actual)
  }


  protected fun assertNotificationAware(vararg projects: ExternalSystemProjectId, notified: Boolean, event: String) {
    val message = when (notified) {
      true -> "Notification must be notified"
      else -> "Notification must be expired"
    }
    assertEquals("$message on $event", notified, notificationAware.isNotificationNotified(*projects))
  }

  protected fun modification(name: String, action: () -> Unit) {
    BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC).batchChangeStarted(myProject, name)
    action()
    BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC).batchChangeCompleted(myProject)
  }
}