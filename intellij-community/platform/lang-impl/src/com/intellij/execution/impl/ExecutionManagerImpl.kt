// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.CommonBundle
import com.intellij.execution.*
import com.intellij.execution.configuration.CompatibilityAwareRunProfile
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfiguration.RestartSingletonResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.statistics.RunConfigurationUsageTriggerCollector
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.internal.statistic.IdeActivity
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.AppUIUtil
import com.intellij.ui.UIBundle
import com.intellij.util.Alarm
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import gnu.trove.THashSet
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.concurrency.Promise
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

private val LOG = logger<ExecutionManagerImpl>()
private val EMPTY_PROCESS_HANDLERS = emptyArray<ProcessHandler>()

class ExecutionManagerImpl(private val project: Project) : ExecutionManager(), Disposable {
  companion object {
    @JvmField
    val EXECUTION_SESSION_ID_KEY = Key.create<Any>("EXECUTION_SESSION_ID_KEY")

    @JvmField
    val EXECUTION_SKIP_RUN = Key.create<Boolean>("EXECUTION_SKIP_RUN")

    @JvmStatic
    fun getInstance(project: Project) = project.service<ExecutionManager>() as ExecutionManagerImpl

    @JvmStatic
    fun isProcessRunning(descriptor: RunContentDescriptor?): Boolean {
      val processHandler = descriptor?.processHandler
      return processHandler != null && !processHandler.isProcessTerminated
    }

    @JvmStatic
    fun stopProcess(descriptor: RunContentDescriptor?) {
      stopProcess(descriptor?.processHandler)
    }

    @JvmStatic
    fun stopProcess(processHandler: ProcessHandler?) {
      if (processHandler == null) {
        return
      }

      processHandler.putUserData(ProcessHandler.TERMINATION_REQUESTED, true)

      if (processHandler is KillableProcess && processHandler.isProcessTerminating) {
        // process termination was requested, but it's still alive
        // in this case 'force quit' will be performed
        processHandler.killProcess()
        return
      }

      if (!processHandler.isProcessTerminated) {
        if (processHandler.detachIsDefault()) {
          processHandler.detachProcess()
        }
        else {
          processHandler.destroyProcess()
        }
      }
    }

    @JvmStatic
    fun getAllDescriptors(project: Project): List<RunContentDescriptor> {
      return project.serviceIfCreated<RunContentManager>()?.allDescriptors ?: emptyList()
    }
  }

  init {
    val connection = ApplicationManager.getApplication().messageBus.connect(this)
    connection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
      override fun projectClosed(project: Project) {
        if (project === this@ExecutionManagerImpl.project) {
          inProgress.clear()
        }
      }
    })
  }

  @set:TestOnly
  @Volatile
  var forceCompilationInTests = false

  private val awaitingTerminationAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)
  private val awaitingRunProfiles = HashMap<RunProfile, ExecutionEnvironment>()
  private val runningConfigurations: MutableList<RunningConfigurationEntry> = ContainerUtil.createLockFreeCopyOnWriteList()

  private val inProgress = Collections.synchronizedSet(THashSet<InProgressEntry>())

  private fun processNotStarted(environment: ExecutionEnvironment) {
    val executorId = environment.executor.id
    inProgress.remove(InProgressEntry(executorId, environment.runner.runnerId))
    project.messageBus.syncPublisher(EXECUTION_TOPIC).processNotStarted(executorId, environment)
  }

  /**
   * Internal usage only. Maybe removed or changed in any moment. No backward compatibility.
   */
  @ApiStatus.Internal
  override fun startRunProfile(environment: ExecutionEnvironment,
                               callback: ProgramRunner.Callback?,
                               starter: () -> Promise<RunContentDescriptor?>) {
    startRunProfile(environment) {
      // errors are handled by startRunProfile
      starter()
        .then { descriptor ->
          if (descriptor != null) {
            descriptor.executionId = environment.executionId

            val toolWindowId = RunContentManager.getInstance(environment.project).getContentDescriptorToolWindowId(environment)
            if (toolWindowId != null) {
              descriptor.contentToolWindowId = toolWindowId
            }

            val settings = environment.runnerAndConfigurationSettings
            if (settings != null) {
              descriptor.isActivateToolWindowWhenAdded = settings.isActivateToolWindowBeforeRun
            }
          }
          callback?.processStarted(descriptor)
          descriptor
        }
    }
  }

  override fun startRunProfile(starter: RunProfileStarter, environment: ExecutionEnvironment) {
    startRunProfile(environment) {
      starter.executeAsync(environment)
    }
  }

  private fun startRunProfile(environment: ExecutionEnvironment, task: () -> Promise<RunContentDescriptor>) {
    val activity = triggerUsage(environment)

    RunManager.getInstance(environment.project).refreshUsagesList(environment.runProfile)

    val project = environment.project
    val reuseContent = RunContentManager.getInstance(project).getReuseContent(environment)
    if (reuseContent != null) {
      reuseContent.executionId = environment.executionId
      environment.contentToReuse = reuseContent
    }

    val executor = environment.executor
    inProgress.add(InProgressEntry(executor.id, environment.runner.runnerId))
    project.messageBus.syncPublisher(EXECUTION_TOPIC).processStartScheduled(executor.id, environment)

    val startRunnable = Runnable {
      if (project.isDisposed) {
        return@Runnable
      }

      project.messageBus.syncPublisher(EXECUTION_TOPIC).processStarting(executor.id, environment)

      fun handleError(e: Throwable) {
        if (e !is ProcessCanceledException) {
          ProgramRunnerUtil.handleExecutionError(project, environment, e, environment.runProfile)
          LOG.debug(e)
        }
        processNotStarted(environment)
      }

      try {
        task()
          .onSuccess { descriptor ->
            AppUIUtil.invokeLaterIfProjectAlive(project) {
              if (descriptor == null) {
                processNotStarted(environment)
                return@invokeLaterIfProjectAlive
              }

              val entry = RunningConfigurationEntry(descriptor, environment.runnerAndConfigurationSettings, executor)
              runningConfigurations.add(entry)
              Disposer.register(descriptor, Disposable { runningConfigurations.remove(entry) })
              if (!descriptor.isHiddenContent) {
                RunContentManager.getInstance(project).showRunContent(executor, descriptor, environment.contentToReuse)
              }
              activity?.stageStarted("ui.shown")

              val processHandler = descriptor.processHandler
              if (processHandler != null) {
                if (!processHandler.isStartNotified) {
                  processHandler.startNotify()
                }
                inProgress.remove(InProgressEntry(executor.id, environment.runner.runnerId))
                project.messageBus.syncPublisher(EXECUTION_TOPIC).processStarted(executor.id, environment, processHandler)

                val listener = ProcessExecutionListener(project, executor.id, environment, processHandler, descriptor, activity)
                processHandler.addProcessListener(listener)

                // Since we cannot guarantee that the listener is added before process handled is start notified,
                // we have to make sure the process termination events are delivered to the clients.
                // Here we check the current process state and manually deliver events, while
                // the ProcessExecutionListener guarantees each such event is only delivered once
                // either by this code, or by the ProcessHandler.
                val terminating = processHandler.isProcessTerminating
                val terminated = processHandler.isProcessTerminated
                if (terminating || terminated) {
                  listener.processWillTerminate(ProcessEvent(processHandler), false /* doesn't matter */)
                  if (terminated) {
                    listener.processTerminated(
                      ProcessEvent(processHandler, if (processHandler.isStartNotified) processHandler.exitCode ?: -1 else -1))
                  }
                }
              }
              environment.contentToReuse = descriptor
            }
          }
          .onError(::handleError)
      }
      catch (e: Throwable) {
        handleError(e)
      }
    }

    if (!forceCompilationInTests && ApplicationManager.getApplication().isUnitTestMode) {
      startRunnable.run()
    }
    else {
      compileAndRun(Runnable {
        ApplicationManager.getApplication().invokeLater(startRunnable, project.disposed)
      }, environment, Runnable {
        if (!project.isDisposed) {
          processNotStarted(environment)
        }
      })
    }
  }

  override fun dispose() {
    for (entry in runningConfigurations) {
      Disposer.dispose(entry.descriptor)
    }
    runningConfigurations.clear()
  }

  @Suppress("OverridingDeprecatedMember")
  override fun getContentManager() = RunContentManager.getInstance(project)

  override fun getRunningProcesses(): Array<ProcessHandler> {
    var handlers: MutableList<ProcessHandler>? = null
    for (descriptor in getAllDescriptors(project)) {
      val processHandler = descriptor.processHandler ?: continue
      if (handlers == null) {
        handlers = SmartList()
      }
      handlers.add(processHandler)
    }
    return handlers?.toTypedArray() ?: EMPTY_PROCESS_HANDLERS
  }

  override fun compileAndRun(startRunnable: Runnable, environment: ExecutionEnvironment, onCancelRunnable: Runnable?) {
    var id = environment.executionId
    if (id == 0L) {
      id = environment.assignNewExecutionId()
    }

    val profile = environment.runProfile
    if (profile !is RunConfiguration) {
      startRunnable.run()
      return
    }

    val beforeRunTasks = doGetBeforeRunTasks(profile)
    if (beforeRunTasks.isEmpty()) {
      startRunnable.run()
      return
    }

    val context = environment.dataContext
    val projectContext = context ?: SimpleDataContext.getProjectContext(project)
    val runBeforeRunExecutorMap = Collections.synchronizedMap(linkedMapOf<BeforeRunTask<*>, Executor>())

    for (task in beforeRunTasks) {
      val provider = BeforeRunTaskProvider.getProvider(project, task.providerId)
      if (provider == null || task !is RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask) {
        continue
      }

      val settings = task.settings
      if (settings != null) {
        // as side-effect here we setup runners list ( required for com.intellij.execution.impl.RunManagerImpl.canRunConfiguration() )
        var executor = if (Registry.`is`("lock.run.executor.for.before.run.tasks", false)) {
          DefaultRunExecutor.getRunExecutorInstance()
        }
        else {
          environment.executor
        }

        val builder = ExecutionEnvironmentBuilder.createOrNull(executor, settings)
        if (builder == null || !RunManagerImpl.canRunConfiguration(settings, executor)) {
          executor = DefaultRunExecutor.getRunExecutorInstance()
          if (!RunManagerImpl.canRunConfiguration(settings, executor)) {
            // we should stop here as before run task cannot be executed at all (possibly it's invalid)
            onCancelRunnable?.run()
            ExecutionUtil.handleExecutionError(environment, ExecutionException("cannot start before run task '$settings'."))
            return
          }
        }
        runBeforeRunExecutorMap.put(task, executor)
      }
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      for (task in beforeRunTasks) {
        if (project.isDisposed) {
          return@executeOnPooledThread
        }

        @Suppress("UNCHECKED_CAST")
        val provider = BeforeRunTaskProvider.getProvider(project, task.providerId) as BeforeRunTaskProvider<BeforeRunTask<*>>?
        if (provider == null) {
          LOG.warn("Cannot find BeforeRunTaskProvider for id='${task.providerId}'")
          continue
        }

        val builder = ExecutionEnvironmentBuilder(environment).contentToReuse(null)
        val executor = runBeforeRunExecutorMap[task]
        if (executor != null) {
          builder.executor(executor)
        }

        val taskEnvironment = builder.build()
        taskEnvironment.executionId = id
        EXECUTION_SESSION_ID_KEY.set(taskEnvironment, id)
        if (!provider.executeTask(projectContext, profile, taskEnvironment, task)) {
          if (onCancelRunnable != null) {
            SwingUtilities.invokeLater(onCancelRunnable)
          }
          return@executeOnPooledThread
        }
      }

      doRun(environment, startRunnable)
    }
  }

  private fun doRun(environment: ExecutionEnvironment, startRunnable: Runnable) {
    val allowSkipRun = environment.getUserData(EXECUTION_SKIP_RUN)
    if (allowSkipRun != null && allowSkipRun) {
      processNotStarted(environment)
      return
    }

    // important! Do not use DumbService.smartInvokeLater here because it depends on modality state
    // and execution of startRunnable could be skipped if modality state check fails

    SwingUtilities.invokeLater {
      if (project.isDisposed) {
        return@invokeLater
      }

      val settings = environment.runnerAndConfigurationSettings
      if (settings != null && !settings.type.isDumbAware && DumbService.isDumb(project)) {
        DumbService.getInstance(project).runWhenSmart(startRunnable)
      }
      else {
        try {
          startRunnable.run()
        }
        catch (ignored: IndexNotReadyException) {
          ExecutionUtil.handleExecutionError(environment, ExecutionException("cannot start while indexing is in progress."))
        }
      }
    }
  }

  override fun restartRunProfile(project: Project,
                                 executor: Executor,
                                 target: ExecutionTarget,
                                 configuration: RunnerAndConfigurationSettings?,
                                 processHandler: ProcessHandler?) {
    val builder = createEnvironmentBuilder(project, executor, configuration)
    if (processHandler != null) {
      for (descriptor in getAllDescriptors(project)) {
        if (descriptor.processHandler === processHandler) {
          builder.contentToReuse(descriptor)
          break
        }
      }
    }
    restartRunProfile(builder.target(target).build())
  }

  override fun restartRunProfile(environment: ExecutionEnvironment) {
    val configuration = environment.runnerAndConfigurationSettings

    val runningIncompatible: List<RunContentDescriptor>
    if (configuration == null) {
      runningIncompatible = emptyList()
    }
    else {
      runningIncompatible = getIncompatibleRunningDescriptors(configuration)
    }

    val contentToReuse = environment.contentToReuse
    val runningOfTheSameType = if (configuration != null && !configuration.configuration.isAllowRunningInParallel) {
      getRunningDescriptors(Condition { configuration === it })
    }
    else if (isProcessRunning(contentToReuse)) {
      listOf(contentToReuse!!)
    }
    else {
      emptyList()
    }

    val runningToStop = ContainerUtil.concat(runningOfTheSameType, runningIncompatible)
    if (runningToStop.isNotEmpty()) {
      if (configuration != null) {
        if (runningOfTheSameType.isNotEmpty() && (runningOfTheSameType.size > 1 || contentToReuse == null || runningOfTheSameType.first() !== contentToReuse)) {
          val result = configuration.configuration.restartSingleton(environment)
          if (result == RestartSingletonResult.NO_FURTHER_ACTION) {
            return
          }
          if (result == RestartSingletonResult.ASK_AND_RESTART && !userApprovesStopForSameTypeConfigurations(environment.project, configuration.name, runningOfTheSameType.size)) {
            return
          }
        }
        if (runningIncompatible.isNotEmpty() && !userApprovesStopForIncompatibleConfigurations(project, configuration.name, runningIncompatible)) {
          return
        }
      }

      for (descriptor in runningToStop) {
        stopProcess(descriptor)
      }
    }

    if (awaitingRunProfiles.get(environment.runProfile) === environment) {
      // defense from rerunning exactly the same ExecutionEnvironment
      return
    }

    awaitingRunProfiles.put(environment.runProfile, environment)

    awaitTermination(object : Runnable {
      override fun run() {
        if (awaitingRunProfiles.get(environment.runProfile) !== environment) {
          // a new rerun has been requested before starting this one, ignore this rerun
          return
        }

        if ((configuration != null && !configuration.type.isDumbAware && DumbService.getInstance(project).isDumb)
            || inProgress.contains(InProgressEntry(environment.executor.id, environment.runner.runnerId))) {
          awaitTermination(this, 100)
          return
        }

        for (descriptor in runningOfTheSameType) {
          val processHandler = descriptor.processHandler
          if (processHandler != null && !processHandler.isProcessTerminated) {
            awaitTermination(this, 100)
            return
          }
        }

        awaitingRunProfiles.remove(environment.runProfile)

        // start() can be called during restartRunProfile() after pretty long 'awaitTermination()' so we have to check if the project is still here
        if (environment.project.isDisposed) {
          return
        }

        val settings = environment.runnerAndConfigurationSettings
        executeConfiguration(environment, settings != null && settings.isEditBeforeRun)
      }
    }, 50)
  }

  @ApiStatus.Internal
  fun executeConfiguration(environment: ExecutionEnvironment,
                           showSettings: Boolean,
                           assignNewId: Boolean = true,
                           callback: ProgramRunner.Callback? = null) {
    val runnerAndConfigurationSettings = environment.runnerAndConfigurationSettings
    val project = environment.project
    var runner = environment.runner
    if (runnerAndConfigurationSettings != null) {
      val targetManager = ExecutionTargetManager.getInstance(project)
      if (!targetManager.doCanRun(runnerAndConfigurationSettings.configuration, environment.executionTarget)) {
        ExecutionUtil.handleExecutionError(environment, ExecutionException(ProgramRunnerUtil.getCannotRunOnErrorMessage(environment.runProfile, environment.executionTarget)))
        return
      }

      if (((showSettings && runnerAndConfigurationSettings.isEditBeforeRun) || !RunManagerImpl.canRunConfiguration(environment)) && !DumbService.isDumb(project)) {
        if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
          return
        }

        while (!RunManagerImpl.canRunConfiguration(environment)) {
          val message = "Configuration is still incorrect. Do you want to edit it again?"
          val title = "Change Configuration Settings"
          if (Messages.showYesNoDialog(project, message, title, CommonBundle.message("button.edit"), ExecutionBundle.message("run.continue.anyway"), Messages.getErrorIcon()) != Messages.YES) {
            break
          }
          if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
            return
          }
        }

        // corresponding runner can be changed after configuration edit
        runner = ProgramRunner.getRunner(environment.executor.id, runnerAndConfigurationSettings.configuration)
                 ?: throw ExecutionException("Cannot find runner for ${environment.runProfile.name}")
      }
    }

    try {
      var effectiveEnvironment = environment
      if (runner != effectiveEnvironment.runner) {
        effectiveEnvironment = ExecutionEnvironmentBuilder(effectiveEnvironment).runner(runner).build()
      }
      if (assignNewId) {
        effectiveEnvironment.assignNewExecutionId()
      }
      runner.execute(effectiveEnvironment, callback)
    }
    catch (e: ExecutionException) {
      ProgramRunnerUtil.handleExecutionError(project, environment, e, runnerAndConfigurationSettings?.configuration)
    }
  }

  override fun isStarting(executorId: String, runnerId: String): Boolean {
    return inProgress.contains(InProgressEntry(executorId, runnerId))
  }

  private fun awaitTermination(request: Runnable, delayMillis: Long) {
    val app = ApplicationManager.getApplication()
    if (app.isUnitTestMode) {
      app.invokeLater(request, ModalityState.any())
    }
    else {
      awaitingTerminationAlarm.addRequest(request, delayMillis)
    }
  }

  private fun getIncompatibleRunningDescriptors(configurationAndSettings: RunnerAndConfigurationSettings): List<RunContentDescriptor> {
    val configurationToCheckCompatibility = configurationAndSettings.configuration
    return getRunningDescriptors(Condition { runningConfigurationAndSettings ->
      val runningConfiguration = runningConfigurationAndSettings.configuration
      if (runningConfiguration is CompatibilityAwareRunProfile) {
        runningConfiguration.mustBeStoppedToRun(configurationToCheckCompatibility)
      }
      else {
        false
      }
    })
  }

  fun getRunningDescriptors(condition: Condition<in RunnerAndConfigurationSettings>): List<RunContentDescriptor> {
    val result = SmartList<RunContentDescriptor>()
    for (entry in runningConfigurations) {
      if (entry.settings != null && condition.value(entry.settings)) {
        val processHandler = entry.descriptor.processHandler
        if (processHandler != null /*&& !processHandler.isProcessTerminating()*/ && !processHandler.isProcessTerminated) {
          result.add(entry.descriptor)
        }
      }
    }
    return result
  }

  fun getDescriptors(condition: Condition<in RunnerAndConfigurationSettings>): List<RunContentDescriptor> {
    val result = SmartList<RunContentDescriptor>()
    for (entry in runningConfigurations) {
      if (entry.settings != null && condition.value(entry.settings)) {
        result.add(entry.descriptor)
      }
    }
    return result
  }

  fun getExecutors(descriptor: RunContentDescriptor): Set<Executor> {
    val result = HashSet<Executor>()
    for (entry in runningConfigurations) {
      if (descriptor === entry.descriptor) {
        result.add(entry.executor)
      }
    }
    return result
  }

  fun getConfigurations(descriptor: RunContentDescriptor): Set<RunnerAndConfigurationSettings> {
    val result = HashSet<RunnerAndConfigurationSettings>()
    for (entry in runningConfigurations) {
      if (descriptor === entry.descriptor && entry.settings != null) {
        result.add(entry.settings)
      }
    }
    return result
  }
}

private fun triggerUsage(environment: ExecutionEnvironment): IdeActivity? {
  val configurationFactory = environment.runnerAndConfigurationSettings?.configuration?.factory ?: return null
  return RunConfigurationUsageTriggerCollector.trigger(environment.project, configurationFactory, environment.executor)
}

private fun createEnvironmentBuilder(project: Project,
                                     executor: Executor,
                                     configuration: RunnerAndConfigurationSettings?): ExecutionEnvironmentBuilder {
  val builder = ExecutionEnvironmentBuilder(project, executor)

  val runner = configuration?.let { ProgramRunner.getRunner(executor.id, it.configuration) }
  if (runner == null && configuration != null) {
    LOG.error("Cannot find runner for ${configuration.name}")
  }
  else if (runner != null) {
    builder.runnerAndSettings(runner, configuration)
  }
  return builder
}

private fun userApprovesStopForSameTypeConfigurations(project: Project, configName: String, instancesCount: Int): Boolean {
  val config = RunManagerImpl.getInstanceImpl(project).config
  if (!config.isRestartRequiresConfirmation) {
    return true
  }

  @Suppress("DuplicatedCode")
  val option = object : DialogWrapper.DoNotAskOption {
    override fun isToBeShown() = config.isRestartRequiresConfirmation

    override fun setToBeShown(value: Boolean, exitCode: Int) {
      config.isRestartRequiresConfirmation = value
    }

    override fun canBeHidden() = true

    override fun shouldSaveOptionsOnCancel() = false

    override fun getDoNotShowMessage(): String {
      return UIBundle.message("dialog.options.do.not.show")
    }
  }
  return Messages.showOkCancelDialog(
    project,
    ExecutionBundle.message("rerun.singleton.confirmation.message", configName, instancesCount),
    ExecutionBundle.message("process.is.running.dialog.title", configName),
    ExecutionBundle.message("rerun.confirmation.button.text"),
    CommonBundle.message("button.cancel"),
    Messages.getQuestionIcon(), option) == Messages.OK
}

private fun userApprovesStopForIncompatibleConfigurations(project: Project,
                                                          configName: String,
                                                          runningIncompatibleDescriptors: List<RunContentDescriptor>): Boolean {
  @Suppress("DuplicatedCode")
  val config = RunManagerImpl.getInstanceImpl(project).config
  @Suppress("DuplicatedCode")
  if (!config.isStopIncompatibleRequiresConfirmation) {
    return true
  }

  @Suppress("DuplicatedCode")
  val option = object : DialogWrapper.DoNotAskOption {
    override fun isToBeShown() = config.isStopIncompatibleRequiresConfirmation

    override fun setToBeShown(value: Boolean, exitCode: Int) {
      config.isStopIncompatibleRequiresConfirmation = value
    }

    override fun canBeHidden() = true

    override fun shouldSaveOptionsOnCancel() = false

    override fun getDoNotShowMessage(): String {
      return UIBundle.message("dialog.options.do.not.show")
    }
  }

  val names = StringBuilder()
  for (descriptor in runningIncompatibleDescriptors) {
    val name = descriptor.displayName
    if (names.isNotEmpty()) {
      names.append(", ")
    }
    names.append(if (name.isNullOrEmpty()) ExecutionBundle.message("run.configuration.no.name") else String.format("'%s'", name))
  }

  return Messages.showOkCancelDialog(
    project,
    ExecutionBundle.message("stop.incompatible.confirmation.message",
      configName, names.toString(), runningIncompatibleDescriptors.size),
    ExecutionBundle.message("incompatible.configuration.is.running.dialog.title", runningIncompatibleDescriptors.size),
    ExecutionBundle.message("stop.incompatible.confirmation.button.text"),
    CommonBundle.message("button.cancel"),
    Messages.getQuestionIcon(), option) == Messages.OK
}

private class ProcessExecutionListener(private val project: Project,
                                       private val executorId: String,
                                       private val environment: ExecutionEnvironment,
                                       private val processHandler: ProcessHandler,
                                       private val descriptor: RunContentDescriptor,
                                       private val activity: IdeActivity?) : ProcessAdapter() {
  private val willTerminateNotified = AtomicBoolean()
  private val terminateNotified = AtomicBoolean()

  override fun processTerminated(event: ProcessEvent) {
    if (project.isDisposed || !terminateNotified.compareAndSet(false, true)) {
      return
    }

    ApplicationManager.getApplication().invokeLater(Runnable {
      val ui = descriptor.runnerLayoutUi
      if (ui != null && !ui.isDisposed) {
        ui.updateActionsNow()
      }
    }, ModalityState.any(), project.disposed)

    project.messageBus.syncPublisher(ExecutionManager.EXECUTION_TOPIC).processTerminated(executorId, environment, processHandler, event.exitCode)

    activity?.finished()

    SaveAndSyncHandler.getInstance().scheduleRefresh()
  }

  override fun processWillTerminate(event: ProcessEvent, shouldNotBeUsed: Boolean) {
    if (project.isDisposed || !willTerminateNotified.compareAndSet(false, true)) {
      return
    }

    project.messageBus.syncPublisher(ExecutionManager.EXECUTION_TOPIC).processTerminating(executorId, environment, processHandler)
  }
}

private data class InProgressEntry(val executorId: String, val runnerId: String)

private data class RunningConfigurationEntry(val descriptor: RunContentDescriptor, val settings: RunnerAndConfigurationSettings?, val executor: Executor)