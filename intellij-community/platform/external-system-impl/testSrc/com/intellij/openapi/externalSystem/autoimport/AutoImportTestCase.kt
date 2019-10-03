// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.test.ExternalSystemTestCase
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.replaceService
import java.io.File
import java.util.concurrent.TimeUnit

abstract class AutoImportTestCase : ExternalSystemTestCase() {
  override fun getTestsTempDir() = "tmp${System.currentTimeMillis()}"

  override fun getExternalSystemConfigFileName() = throw UnsupportedOperationException()

  private val notificationAware get() = ProjectNotificationAware.getInstance(myProject)
  private val projectTracker get() = ExternalSystemProjectTracker.getInstance(myProject) as ProjectTracker

  private fun <R> traceableAction(action: () -> R): R {
    val result = action()
    projectTracker.waitForAsyncTasksCompletion(1, TimeUnit.MINUTES)
    return result
  }

  protected fun findOrCreateFile(relativePath: String, content: String? = null) = traceableAction {
    when (content) {
      null -> createProjectSubFile(relativePath)!!
      else -> createProjectSubFile(relativePath, content)!!
    }
  }

  protected fun findOrCreateDirectory(relativePath: String) = traceableAction {
    createProjectSubDir(relativePath)!!
  }

  private fun <R> update(update: () -> R): R = traceableAction {
    WriteCommandAction.runWriteCommandAction(myProject, Computable { update() })
  }

  private fun getPath(relativePath: String) = "$projectPath/$relativePath"

  private fun getFile(relativePath: String) = File(projectPath, relativePath)

  protected fun VirtualFile.rename(name: String) =
    update { rename(null, name) }

  protected fun VirtualFile.copy(relativePath: String) =
    update {
      val newFile = getFile(relativePath)
      val parent = VfsUtil.findFileByIoFile(newFile.parentFile, true)!!
      copy(null, parent, newFile.name)
    }

  protected fun VirtualFile.move(parent: VirtualFile) =
    update { move(null, parent) }

  protected fun VirtualFile.removeContent() =
    update { VfsUtil.saveText(this, "") }

  protected fun VirtualFile.replaceContent(content: String) =
    update { VfsUtil.saveText(this, content) }

  protected fun VirtualFile.appendString(string: String) =
    update { VfsUtil.saveText(this, VfsUtil.loadText(this) + string) }

  protected fun VirtualFile.replaceString(old: String, new: String) =
    update { VfsUtil.saveText(this, VfsUtil.loadText(this).replace(old, new)) }

  protected fun VirtualFile.delete() =
    update { delete(null) }

  protected fun Document.save() =
    update { FileDocumentManager.getInstance().saveDocument(this) }

  protected fun Document.replaceContent(content: String) =
    update { replaceString(0, text.length, content) }

  protected fun Document.replaceString(old: String, new: String) =
    update {
      val startOffset = text.indexOf(old)
      val endOffset = startOffset + old.length
      replaceString(startOffset, endOffset, new)
    }

  protected fun register(projectAware: ExternalSystemProjectAware) = traceableAction {
    projectTracker.register(projectAware)
  }

  protected fun remove(projectId: ExternalSystemProjectId) = traceableAction {
    projectTracker.remove(projectId)
  }

  protected fun refreshProject() = traceableAction {
    projectTracker.scheduleProjectRefresh()
  }

  protected fun loadState(state: ProjectTracker.State) = traceableAction {
    projectTracker.loadState(state)
  }

  protected fun getState() = projectTracker.state

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
      true -> "Notification must be notified for $projects"
      else -> "Notification must be expired"
    }
    assertEquals("$message on $event", notified, notificationAware.isNotificationNotified(*projects))
  }

  protected fun modification(action: () -> Unit) = traceableAction {
    BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC).batchChangeStarted(myProject, "modification")
    action()
    BackgroundTaskUtil.syncPublisher(BatchFileChangeListener.TOPIC).batchChangeCompleted(myProject)
  }

  private fun <S : Any, R> ComponentManager.replaceService(aClass: Class<S>, service: S, action: () -> R): R {
    val temporaryDisposable = Disposer.newDisposable()
    try {
      replaceService(aClass, service, temporaryDisposable)
      return action()
    }
    finally {
      Disposer.dispose(temporaryDisposable)
    }
  }

  protected fun simpleTest(fileRelativePath: String,
                           content: String? = null,
                           state: ProjectTracker.State = ProjectTracker.State(),
                           test: SimpleTestBench.(VirtualFile) -> Unit): ProjectTracker.State {
    return myProject.replaceService(ExternalSystemProjectTracker::class.java, ProjectTracker(myProject)) {
      val systemId = ProjectSystemId("External System")
      val projectId = ExternalSystemProjectId(systemId, projectPath)
      val projectAware = MockProjectAware(projectId)
      loadState(state)
      assertTrue(projectTracker.isInitialized())
      val file = findOrCreateFile(fileRelativePath, content)
      projectAware.settingsFiles.add(file.path)
      register(projectAware)
      SimpleTestBench(projectAware).test(file)
      val newState = getState()
      remove(projectAware.projectId)
      newState
    }
  }

  protected inner class SimpleTestBench(private val projectAware: MockProjectAware) {

    fun registerProjectAware() = register(projectAware)

    fun removeProjectAware() = remove(projectAware.projectId)

    fun registerSettingsFile(file: VirtualFile) = projectAware.settingsFiles.add(file.path)

    fun registerSettingsFile(relativePath: String) = projectAware.settingsFiles.add(getPath(relativePath))

    fun assertState(refresh: Int? = null,
                    subscribe: Int? = null,
                    unsubscribe: Int? = null,
                    notified: Boolean,
                    event: String) {
      assertProjectAware(projectAware, refresh, subscribe, unsubscribe, event)
      assertNotificationAware(notified = notified, event = event)
    }
  }
}