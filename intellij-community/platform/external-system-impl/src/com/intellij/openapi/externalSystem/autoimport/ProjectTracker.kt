// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus.CANCEL
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus.FAILURE
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus
import com.intellij.openapi.externalSystem.util.CompoundParallelOperationTrace
import com.intellij.openapi.externalSystem.util.properties.BooleanProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.LocalTimeCounter.currentTime
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.MergingUpdateQueue.ANY_COMPONENT
import com.intellij.util.ui.update.Update
import java.util.concurrent.ConcurrentHashMap

@State(name = "ExternalSystemProjectTracker")
class ProjectTracker(private val project: Project) : ExternalSystemProjectTracker, PersistentStateComponent<ProjectTracker.State> {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

  private val projectStates = ConcurrentHashMap<State.Id, State.Project>()
  private val projectDataMap = ConcurrentHashMap<ExternalSystemProjectId, ProjectData>()
  private val disableProperty = BooleanProperty(false)
  private val projectChangeOperation = CompoundParallelOperationTrace<Nothing?>()
  private val projectRefreshOperation = CompoundParallelOperationTrace<Long>()
  private val dispatcher = MergingUpdateQueue("project tracker", 500, true, ANY_COMPONENT, this)

  override var isDisabled by disableProperty

  private fun createProjectChangesListener() =
    object : ProjectBatchFileChangeListener(project) {
      override fun batchChangeStarted(activityName: String?) =
        projectChangeOperation.startOperation(null)

      override fun batchChangeCompleted() =
        projectChangeOperation.finishTask(null)
    }

  private fun createProjectRefreshListener(projectData: ProjectData) =
    object : ExternalSystemProjectRefreshListener {
      val id = currentTime()

      override fun beforeProjectRefresh() {
        projectRefreshOperation.startOperation(id)
        projectData.settingsTracker.applyChanges()
        projectData.status.markSynchronized(currentTime())
      }

      override fun afterProjectRefresh(status: ExternalSystemRefreshStatus) {
        if (status == FAILURE || status == CANCEL) {
          projectData.status.markDirty(currentTime())
        }
        projectRefreshOperation.finishTask(id)
      }
    }

  override fun scheduleProjectRefresh() {
    LOG.debug("Schedule project refresh")
    dispatcher.queue(object : Update("update") {
      override fun run() {
        LOG.debug("Dispatch project refresh")
        if (isDisabled || !projectChangeOperation.isOperationCompleted()) return
        for (projectData in projectDataMap.values) {
          if (!projectData.status.isUpToDate()) {
            projectData.projectAware.refreshProject()
          }
        }
      }
    })
  }

  override fun scheduleProjectNotificationUpdate() {
    LOG.debug("Schedule notification status update")
    dispatcher.queue(object : Update("notify") {
      override fun run() {
        LOG.debug("Dispatch notification status update")
        if (isDisabled || !projectChangeOperation.isOperationCompleted()) return
        val notificationAware = ExternalSystemProjectNotificationAware.getInstance(project)
        for ((projectId, data) in projectDataMap) {
          when (data.status.isUpToDate()) {
            true -> notificationAware.notificationExpire(projectId)
            else -> notificationAware.notificationNotify(data.projectAware)
          }
        }
      }
    })
  }

  override fun register(projectAware: ExternalSystemProjectAware) {
    val projectId = projectAware.projectId
    val projectStatus = ProjectStatus(debugName = projectId.readableName)
    val parentDisposable = Disposer.newDisposable(projectId.toString())
    val settingsTracker = ProjectSettingsTracker(project, projectStatus, projectAware, parentDisposable)
    val projectData = ProjectData(projectStatus, projectAware, settingsTracker, parentDisposable)
    val notificationAware = ExternalSystemProjectNotificationAware.getInstance(project)

    projectDataMap[projectId] = projectData

    Disposer.register(this, parentDisposable)
    VirtualFileManager.getInstance().addAsyncFileListener(settingsTracker, this)
    projectAware.subscribe(createProjectRefreshListener(projectData), parentDisposable)
    Disposer.register(parentDisposable, Disposable { notificationAware.notificationExpire(projectId) })

    val projectState = projectStates.remove(projectId.getState())
    when (val state = projectState?.settingsTracker) {
      null -> projectStatus.markDirty(currentTime())
      else -> settingsTracker.loadState(state)
    }
    scheduleProjectRefresh()
  }

  override fun remove(id: ExternalSystemProjectId) {
    val projectData = projectDataMap.remove(id)
    if (projectData == null) {
      LOG.warn(String.format("Project isn't registered by id=%s", id))
      return
    }
    Disposer.dispose(projectData.parentDisposable)
  }

  override fun markDirty(id: ExternalSystemProjectId) {
    val projectData = projectDataMap[id]
    if (projectData == null) {
      LOG.warn(String.format("Project isn't registered by id=%s", id))
      return
    }
    projectData.status.markDirty(currentTime())
  }

  override fun dispose() {
    projectDataMap.clear()
  }

  override fun getState(): State {
    val projectSettingsTrackerStates = projectDataMap.values
      .map { it.projectAware.projectId.getState() to it.getState() }
      .toMap()
    return State(projectSettingsTrackerStates)
  }

  override fun loadState(state: State) {
    projectStates.putAll(state.projectSettingsTrackerStates)
  }

  private fun reset() {
    projectDataMap.values.forEach {
      it.settingsTracker.applyChanges()
      it.status.markSynchronized(currentTime())
    }
  }

  init {
    val connections = ApplicationManager.getApplication().messageBus.connect(this)
    connections.subscribe(BatchFileChangeListener.TOPIC, createProjectChangesListener())
    val notificationAware = ExternalSystemProjectNotificationAware.getInstance(project)
    projectRefreshOperation.beforeOperation { LOG.debug("Project refresh started") }
    projectRefreshOperation.beforeOperation { notificationAware.notificationExpire() }
    projectRefreshOperation.afterOperation { scheduleProjectNotificationUpdate() }
    projectRefreshOperation.afterOperation { LOG.debug("Project refresh finished") }
    projectChangeOperation.beforeOperation { LOG.debug("Project change started") }
    projectChangeOperation.beforeOperation { notificationAware.notificationExpire() }
    projectChangeOperation.afterOperation { scheduleProjectRefresh() }
    projectChangeOperation.afterOperation { LOG.debug("Project change finished") }
    disableProperty.afterSet { LOG.debug("Project tracker is disabled") }
    disableProperty.afterReset { LOG.debug("Project tracker is enabled") }
    disableProperty.afterReset { reset() }
  }

  fun <Id> CompoundParallelOperationTrace<Id>.startOperation(taskId: Id) {
    startOperation()
    startTask(taskId)
  }

  private fun ProjectData.getState() = State.Project(status.isDirty() || isDisabled, settingsTracker.getState())

  private fun ExternalSystemProjectId.getState() = State.Id(systemId.id, externalProjectPath)

  private data class ProjectData(
    val status: ProjectStatus,
    val projectAware: ExternalSystemProjectAware,
    val settingsTracker: ProjectSettingsTracker,
    val parentDisposable: Disposable
  )

  data class State(var projectSettingsTrackerStates: Map<Id, Project> = emptyMap()) {
    data class Id(var systemId: String? = null, var externalProjectPath: String? = null)
    data class Project(var isDirty: Boolean = false, var settingsTracker: ProjectSettingsTracker.State? = null)
  }
}