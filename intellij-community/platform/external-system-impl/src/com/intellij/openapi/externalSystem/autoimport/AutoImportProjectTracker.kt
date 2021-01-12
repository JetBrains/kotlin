// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTrackerSettings.AutoReloadType
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus.SUCCESS
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.EXTERNAL
import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType.INTERNAL
import com.intellij.openapi.externalSystem.autoimport.update.PriorityEatUpdate
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.observable.operations.AnonymousParallelOperationTrace
import com.intellij.openapi.observable.operations.CompoundParallelOperationTrace
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.BooleanProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.LocalTimeCounter.currentTime
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import kotlin.streams.asStream

@State(name = "ExternalSystemProjectTracker", storages = [Storage(CACHE_FILE)])
class AutoImportProjectTracker(private val project: Project) : ExternalSystemProjectTracker, PersistentStateComponent<AutoImportProjectTracker.State> {
  @Suppress("unused")
  private val debugThrowable = Throwable("Initialized with project=(${project.isDisposed}, ${Disposer.isDisposed(project)}, $project)")

  private val AUTO_REPARSE_DELAY = DaemonCodeAnalyzerSettings.getInstance().autoReparseDelay
  private val AUTO_RELOAD_DELAY = 2000

  private val settings get() = AutoImportProjectTrackerSettings.getInstance(project)
  private val projectStates = ConcurrentHashMap<State.Id, State.Project>()
  private val projectDataMap = ConcurrentHashMap<ExternalSystemProjectId, ProjectData>()
  private val isDisabled = AtomicBooleanProperty(ApplicationManager.getApplication().isUnitTestMode)
  private val asyncChangesProcessingProperty = AtomicBooleanProperty(!ApplicationManager.getApplication().isHeadlessEnvironment)
  private val projectChangeOperation = AnonymousParallelOperationTrace(debugName = "Project change operation")
  private val projectRefreshOperation = CompoundParallelOperationTrace<String>(debugName = "Project refresh operation")
  private val dispatcher = MergingUpdateQueue("AutoImportProjectTracker.dispatcher", AUTO_REPARSE_DELAY, false, null, project)
  private val delayDispatcher = MergingUpdateQueue("AutoImportProjectTracker.delayDispatcher", AUTO_RELOAD_DELAY, false, null, project)
  private val backgroundExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("AutoImportProjectTracker.backgroundExecutor", 1)

  var isAsyncChangesProcessing by asyncChangesProcessingProperty

  private fun createProjectChangesListener() =
    object : ProjectBatchFileChangeListener(project) {
      override fun batchChangeStarted(activityName: String?) =
        projectChangeOperation.startTask()

      override fun batchChangeCompleted() =
        projectChangeOperation.finishTask()
    }

  private fun createProjectRefreshListener(projectData: ProjectData) =
    object : ExternalSystemProjectRefreshListener {
      val id = "ProjectTracker: ${projectData.projectAware.projectId.readableName}"

      override fun beforeProjectRefresh() {
        projectRefreshOperation.startTask(id)
        projectData.status.markSynchronized(currentTime())
        projectData.isActivated = true
      }

      override fun afterProjectRefresh(status: ExternalSystemRefreshStatus) {
        if (status != SUCCESS) projectData.status.markBroken(currentTime())
        projectRefreshOperation.finishTask(id)
      }
    }

  override fun scheduleProjectRefresh() {
    LOG.debug("Schedule project refresh", Throwable())
    dispatcher.queue(PriorityEatUpdate(0) {
      refreshProject(smart = false)
    })
  }

  override fun scheduleProjectNotificationUpdate() {
    LOG.debug("Schedule notification status update")
    dispatcher.queue(PriorityEatUpdate(2) {
      updateProjectNotification()
    })
  }

  fun scheduleChangeProcessing() {
    LOG.debug("Schedule change processing")
    dispatcher.queue(PriorityEatUpdate(1) {
      processChanges()
    })
  }

  private fun delay(action: () -> Unit) {
    delayDispatcher.queue(Update.create(this, action))
  }

  private fun processChanges() {
    when (settings.autoReloadType) {
      AutoReloadType.ALL -> when (getModificationType()) {
        INTERNAL -> delay { refreshProject(smart = true) }
        EXTERNAL -> delay { refreshProject(smart = true) }
        null -> updateProjectNotification()
      }
      AutoReloadType.SELECTIVE -> when (getModificationType()) {
        INTERNAL -> updateProjectNotification()
        EXTERNAL -> delay { refreshProject(smart = true) }
        null -> updateProjectNotification()
      }
      AutoReloadType.NONE -> updateProjectNotification()
    }
  }

  private fun refreshProject(smart: Boolean) {
    LOG.debug("Incremental project refresh")
    if (isDisabled.get() || Registry.`is`("external.system.auto.import.disabled")) return
    if (!projectChangeOperation.isOperationCompleted()) return
    if (smart && !projectRefreshOperation.isOperationCompleted()) return
    var isSkippedProjectRefresh = true
    for (projectData in projectDataMap.values) {
      val projectId = projectData.projectAware.projectId.readableName
      val isAllowAutoReload = !smart || projectData.isActivated
      if (isAllowAutoReload && !projectData.isUpToDate()) {
        isSkippedProjectRefresh = false
        LOG.debug("$projectId: Project refresh")
        val context = ProjectReloadContext(!smart)
        projectData.projectAware.reloadProject(context)
      }
      else {
        LOG.debug("$projectId: Skip project refresh")
      }
    }
    if (isSkippedProjectRefresh) {
      updateProjectNotification()
    }
  }

  private fun updateProjectNotification() {
    LOG.debug("Notification status update")
    if (isDisabled.get() || Registry.`is`("external.system.auto.import.disabled")) return
    val notificationAware = ProjectNotificationAware.getInstance(project)
    for ((projectId, data) in projectDataMap) {
      when (data.isUpToDate()) {
        true -> notificationAware.notificationExpire(projectId)
        else -> notificationAware.notificationNotify(data.projectAware)
      }
    }
  }

  private fun getModificationType(): ModificationType? {
    return projectDataMap.values
      .asSequence()
      .mapNotNull { it.getModificationType() }
      .asStream()
      .reduce(ModificationType::merge)
      .orElse(null)
  }

  override fun register(projectAware: ExternalSystemProjectAware) {
    val projectId = projectAware.projectId
    val activationProperty = AtomicBooleanProperty(false)
    val projectStatus = ProjectStatus(debugName = projectId.readableName)
    val parentDisposable = Disposer.newDisposable(projectId.readableName)
    val settingsTracker = ProjectSettingsTracker(project, this, backgroundExecutor, projectAware, parentDisposable)
    val projectData = ProjectData(projectStatus, activationProperty, projectAware, settingsTracker, parentDisposable)
    val notificationAware = ProjectNotificationAware.getInstance(project)

    projectDataMap[projectId] = projectData

    val id = "ProjectSettingsTracker: ${projectData.projectAware.projectId.readableName}"
    settingsTracker.beforeApplyChanges { projectRefreshOperation.startTask(id) }
    settingsTracker.afterApplyChanges { projectRefreshOperation.finishTask(id) }
    activationProperty.afterSet({ scheduleChangeProcessing() }, parentDisposable)

    Disposer.register(project, parentDisposable)
    projectAware.subscribe(createProjectRefreshListener(projectData), parentDisposable)
    Disposer.register(parentDisposable, Disposable { notificationAware.notificationExpire(projectId) })

    loadState(projectId, projectData)
  }

  override fun activate(id: ExternalSystemProjectId) {
    val projectData = projectDataMap(id) { get(it) } ?: return
    projectData.isActivated = true
  }

  override fun remove(id: ExternalSystemProjectId) {
    val projectData = projectDataMap.remove(id) ?: return
    Disposer.dispose(projectData.parentDisposable)
  }

  override fun markDirty(id: ExternalSystemProjectId) {
    val projectData = projectDataMap(id) { get(it) } ?: return
    projectData.status.markDirty(currentTime())
  }

  private fun projectDataMap(
    id: ExternalSystemProjectId,
    action: MutableMap<ExternalSystemProjectId, ProjectData>.(ExternalSystemProjectId) -> ProjectData?
  ): ProjectData? {
    val projectData = projectDataMap.action(id)
    if (projectData == null) {
      LOG.warn(String.format("Project isn't registered by id=%s", id), Throwable())
    }
    return projectData
  }

  override fun getState(): State {
    val projectSettingsTrackerStates = projectDataMap.asSequence()
      .map { (id, data) -> id.getState() to data.getState() }
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
      projectData.status.markDirty(currentTime(), EXTERNAL)
      scheduleChangeProcessing()
      return
    }
    projectData.settingsTracker.loadState(settingsTrackerState)
    projectData.settingsTracker.refreshChanges()
  }

  override fun initializeComponent() {
    LOG.debug("Project tracker initialization")
    ApplicationManager.getApplication().messageBus.connect(project).subscribe(BatchFileChangeListener.TOPIC, createProjectChangesListener())
    dispatcher.setRestartTimerOnAdd(true)
    dispatcher.isPassThrough = !isAsyncChangesProcessing
    dispatcher.activate()
    delayDispatcher.setRestartTimerOnAdd(true)
    delayDispatcher.isPassThrough = !isAsyncChangesProcessing
    delayDispatcher.activate()
  }

  @TestOnly
  fun getActivatedProjects() =
    projectDataMap.values
      .filter { it.isActivated }
      .map { it.projectAware.projectId }
      .toSet()

  /**
   * Enables auto-import in tests
   * Note: project tracker automatically enabled out of tests
   */
  @TestOnly
  fun enableAutoImportInTests() {
    isDisabled.set(false)
  }

  init {
    val notificationAware = ProjectNotificationAware.getInstance(project)
    projectRefreshOperation.beforeOperation { LOG.debug("Project refresh started") }
    projectRefreshOperation.beforeOperation { notificationAware.notificationExpire() }
    projectRefreshOperation.afterOperation { scheduleChangeProcessing() }
    projectRefreshOperation.afterOperation { LOG.debug("Project refresh finished") }
    projectChangeOperation.beforeOperation { LOG.debug("Project change started") }
    projectChangeOperation.beforeOperation { notificationAware.notificationExpire() }
    projectChangeOperation.afterOperation { scheduleChangeProcessing() }
    projectChangeOperation.afterOperation { LOG.debug("Project change finished") }
    settings.autoReloadTypeProperty.afterChange { scheduleChangeProcessing() }
    asyncChangesProcessingProperty.afterChange { dispatcher.isPassThrough = !it }
    asyncChangesProcessingProperty.afterChange { delayDispatcher.isPassThrough = !it }
  }

  private fun ProjectData.getState() = State.Project(status.isDirty(), settingsTracker.getState())

  private fun ProjectSystemId.getState() = id

  private fun ExternalSystemProjectId.getState() = State.Id(systemId.getState(), externalProjectPath)

  private data class ProjectData(
    val status: ProjectStatus,
    val activationProperty: BooleanProperty,
    val projectAware: ExternalSystemProjectAware,
    val settingsTracker: ProjectSettingsTracker,
    val parentDisposable: Disposable
  ) {
    var isActivated by activationProperty

    fun isUpToDate() = status.isUpToDate() && settingsTracker.isUpToDate()

    fun getModificationType(): ModificationType? {
      val trackerModificationType = status.getModificationType()
      val settingsTrackerModificationType = settingsTracker.getModificationType()
      return when {
        trackerModificationType == null -> settingsTrackerModificationType
        settingsTrackerModificationType == null -> trackerModificationType
        else -> settingsTrackerModificationType.merge(trackerModificationType)
      }
    }
  }

  data class State(var projectSettingsTrackerStates: Map<Id, Project> = emptyMap()) {
    data class Id(var systemId: String? = null, var externalProjectPath: String? = null)
    data class Project(
      var isDirty: Boolean = false,
      var settingsTracker: ProjectSettingsTracker.State? = null
    )
  }

  private data class ProjectReloadContext(override val isExplicitReload: Boolean) : ExternalSystemProjectReloadContext

  companion object {
    private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

    @TestOnly
    @JvmStatic
    fun getInstance(project: Project): AutoImportProjectTracker {
      return ExternalSystemProjectTracker.getInstance(project) as AutoImportProjectTracker
    }
  }
}