// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.ide.file.BatchFileChangeListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros.CACHE_FILE
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus.SUCCESS
import com.intellij.openapi.externalSystem.autoimport.ProjectTracker.ModificationType.EXTERNAL
import com.intellij.openapi.externalSystem.autoimport.ProjectTracker.ModificationType.INTERNAL
import com.intellij.openapi.externalSystem.service.project.autoimport.ProjectStatus
import com.intellij.openapi.observable.operations.CompoundParallelOperationTrace
import com.intellij.openapi.observable.operations.AnonymousParallelOperationTrace
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.LocalTimeCounter.currentTime
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.MergingUpdateQueue.ANY_COMPONENT
import com.intellij.util.ui.update.Update
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

@State(name = "ExternalSystemProjectTracker", storages = [Storage(CACHE_FILE)])
class ProjectTracker(private val project: Project) : ExternalSystemProjectTracker, PersistentStateComponent<ProjectTracker.State> {

  private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")
  private val AUTO_REPARSE_DELAY get() = DaemonCodeAnalyzerSettings.getInstance().autoReparseDelay

  private val projectStates = ConcurrentHashMap<State.Id, State.Project>()
  private val projectDataMap = ConcurrentHashMap<ExternalSystemProjectId, ProjectData>()
  private val isDisabled = AtomicBooleanProperty(ApplicationManager.getApplication().isUnitTestMode)
  private val initializationProperty = AtomicBooleanProperty(false)
  private val projectChangeOperation = AnonymousParallelOperationTrace(debugName = "Project change operation")
  private val projectRefreshOperation = CompoundParallelOperationTrace<String>(debugName = "Project refresh operation")
  private val dispatcher = MergingUpdateQueue("project tracker", AUTO_REPARSE_DELAY, false, ANY_COMPONENT, this)

  private fun createProjectChangesListener() =
    object : ProjectBatchFileChangeListener(project) {
      override fun batchChangeStarted(activityName: String?) {
        projectChangeOperation.startOperation()
        projectChangeOperation.startTask()
      }

      override fun batchChangeCompleted() =
        projectChangeOperation.finishTask()
    }

  private fun createProjectRefreshListener(projectData: ProjectData) =
    object : ExternalSystemProjectRefreshListener {
      val id = "ProjectTracker: ${projectData.projectAware.projectId.readableName}"

      override fun beforeProjectRefresh() {
        projectRefreshOperation.startOperation()
        projectRefreshOperation.startTask(id)
        projectData.status.markSynchronized(currentTime())
      }

      override fun afterProjectRefresh(status: ExternalSystemRefreshStatus) {
        if (status != SUCCESS) projectData.status.markDirty(currentTime())
        projectRefreshOperation.finishTask(id)
      }
    }

  override fun scheduleProjectRefresh() {
    LOG.debug("Schedule project refresh")
    dispatcher.queue(object : Update("update") {
      override fun run() {
        refreshProject()
      }
    })
  }

  override fun scheduleProjectNotificationUpdate() {
    LOG.debug("Schedule notification status update")
    dispatcher.queue(object : Update("notify") {
      override fun run() {
        updateProjectNotification()
      }
    })
  }

  fun scheduleChangeProcessing() {
    LOG.debug("Schedule change processing")
    dispatcher.queue(object : Update("notify") {
      override fun run() {
        when (getModificationType()) {
          INTERNAL -> updateProjectNotification()
          EXTERNAL -> refreshProject()
          null -> updateProjectNotification()
        }
      }
    })
  }

  private fun refreshProject() {
    LOG.debug("Incremental project refresh")
    if (isDisabled.get()) return
    if (!projectChangeOperation.isOperationCompleted()) return
    for (projectData in projectDataMap.values) {
      val projectId = projectData.projectAware.projectId.readableName
      if (!projectData.isUpToDate()) {
        LOG.debug("$projectId: Project refresh")
        projectData.projectAware.refreshProject()
      }
      else {
        LOG.debug("$projectId: Skip project refresh")
      }
    }
  }

  private fun updateProjectNotification() {
    LOG.debug("Notification status update")
    val notificationAware = ProjectNotificationAware.getInstance(project)
    for ((projectId, data) in projectDataMap) {
      when (data.isUpToDate()) {
        true -> notificationAware.notificationExpire(projectId)
        else -> notificationAware.notificationNotify(data.projectAware)
      }
    }
  }

  private fun getModificationType(): ModificationType? {
    val owners = projectDataMap.values
      .mapNotNull { it.getModificationType() }
      .toSet()
    return when {
      INTERNAL in owners -> INTERNAL
      EXTERNAL in owners -> EXTERNAL
      else -> null
    }
  }

  override fun register(projectAware: ExternalSystemProjectAware) {
    val projectId = projectAware.projectId
    val projectStatus = ProjectStatus(debugName = projectId.readableName)
    val parentDisposable = Disposer.newDisposable(projectId.readableName)
    val settingsTracker = ProjectSettingsTracker(project, this, projectAware, parentDisposable)
    val projectData = ProjectData(projectStatus, projectAware, settingsTracker, parentDisposable)
    val notificationAware = ProjectNotificationAware.getInstance(project)

    projectDataMap[projectId] = projectData

    val id = "ProjectSettingsTracker: ${projectData.projectAware.projectId.readableName}"
    settingsTracker.beforeApplyChanges { projectRefreshOperation.startTask(id) }
    settingsTracker.afterApplyChanges { projectRefreshOperation.finishTask(id) }

    Disposer.register(this, parentDisposable)
    projectAware.subscribe(createProjectRefreshListener(projectData), parentDisposable)
    Disposer.register(parentDisposable, Disposable { notificationAware.notificationExpire(projectId) })

    loadState(projectId, projectData)
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
    projectDataMap.forEach { (id, data) -> loadState(id, data) }
  }

  private fun loadState(projectId: ExternalSystemProjectId, projectData: ProjectData) {
    val projectState = projectStates.remove(projectId.getState())
    val settingsTrackerState = projectState?.settingsTracker
    if (settingsTrackerState == null || projectState.isDirty) {
      projectData.status.markDirty(currentTime())
      scheduleProjectRefresh()
      return
    }
    projectData.settingsTracker.loadState(settingsTrackerState)
    projectData.settingsTracker.refreshChanges()
  }

  fun initialize() = initializationProperty.set()

  private fun init() {
    LOG.debug("Project tracker initialization")
    val connections = ApplicationManager.getApplication().messageBus.connect(this)
    connections.subscribe(BatchFileChangeListener.TOPIC, createProjectChangesListener())
    dispatcher.activate()
  }

  @TestOnly
  override fun enableAutoImportInTests() {
    isDisabled.set(false)
  }

  init {
    dispatcher.usePassThroughInUnitTestMode()
    val notificationAware = ProjectNotificationAware.getInstance(project)
    projectRefreshOperation.beforeOperation { LOG.debug("Project refresh started") }
    projectRefreshOperation.beforeOperation { notificationAware.notificationExpire() }
    projectRefreshOperation.afterOperation { scheduleProjectNotificationUpdate() }
    projectRefreshOperation.afterOperation { LOG.debug("Project refresh finished") }
    projectChangeOperation.beforeOperation { LOG.debug("Project change started") }
    projectChangeOperation.beforeOperation { notificationAware.notificationExpire() }
    projectChangeOperation.afterOperation { scheduleChangeProcessing() }
    projectChangeOperation.afterOperation { LOG.debug("Project change finished") }
    isDisabled.afterReset { scheduleProjectRefresh() }
    initializationProperty.afterSet { init() }
    projectRefreshOperation.afterOperation { initializationProperty.set() }
  }

  private fun ProjectData.getState() = State.Project(status.isDirty(), settingsTracker.getState())

  private fun ExternalSystemProjectId.getState() = State.Id(systemId.id, externalProjectPath)

  private data class ProjectData(
    val status: ProjectStatus,
    val projectAware: ExternalSystemProjectAware,
    val settingsTracker: ProjectSettingsTracker,
    val parentDisposable: Disposable
  ) {
    fun isUpToDate() = status.isUpToDate() && settingsTracker.isUpToDate()

    fun getModificationType() = settingsTracker.getModificationType()
  }

  data class State(var projectSettingsTrackerStates: Map<Id, Project> = emptyMap()) {
    data class Id(var systemId: String? = null, var externalProjectPath: String? = null)
    data class Project(
      var isDirty: Boolean = false,
      var settingsTracker: ProjectSettingsTracker.State? = null
    )
  }

  enum class ModificationType { EXTERNAL, INTERNAL }
}