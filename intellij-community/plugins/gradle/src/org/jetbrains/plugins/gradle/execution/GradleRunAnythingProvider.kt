// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY
import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.RunAnythingContext.*
import com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject
import com.intellij.ide.actions.runAnything.activity.RunAnythingProvider
import com.intellij.ide.actions.runAnything.activity.RunAnythingProviderBase
import com.intellij.ide.actions.runAnything.items.RunAnythingHelpItem
import com.intellij.ide.util.gotoByName.GotoClassModel2
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.findProjectData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getChildren
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil.*
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.intellij.util.execution.ParametersListUtil
import com.intellij.util.indexing.FindSymbolParameters
import groovyjarjarcommonscli.Option
import icons.GradleIcons
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsProvider
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.util.*
import javax.swing.Icon
import kotlin.collections.LinkedHashMap


class GradleRunAnythingProvider : RunAnythingProviderBase<String>() {

  override fun getCommand(value: String) = value

  override fun getIcon(value: String): Icon? = GradleIcons.Gradle

  override fun getHelpItem(dataContext: DataContext) =
    RunAnythingHelpItem(helpCommandPlaceholder, helpCommand, helpDescription, helpIcon)

  override fun getHelpGroupTitle() = "Gradle"

  override fun getCompletionGroupTitle() = "Gradle tasks"

  override fun getHelpCommandPlaceholder() = "gradle <taskName...> <--option-name...>"

  override fun getHelpCommand() = HELP_COMMAND

  override fun getHelpIcon(): Icon? = GradleIcons.Gradle

  override fun getMainListItem(dataContext: DataContext, value: String) =
    RunAnythingGradleItem(getCommand(value), getIcon(value))

  override fun findMatchingValue(dataContext: DataContext, pattern: String) =
    if (pattern.startsWith(helpCommand)) getCommand(pattern) else null

  override fun getExecutionContexts(dataContext: DataContext): List<RunAnythingContext> {
    return super.getExecutionContexts(dataContext).filter {
      it !is ModuleContext || !it.module.isSourceRoot()
    }
  }

  override fun getValues(dataContext: DataContext, pattern: String): List<String> {
    val commandLine = parseCommandLine(pattern) ?: return emptyList()
    val context = createContext(dataContext)
    val tasksVariants = completeTasks(commandLine, context)
    val taskOptionsVariants = completeTaskOptions(commandLine, context)
    val taskClassArgumentsVariants = completeTaskClassArguments(commandLine, context)
    val longOptionsVariants = completeOptions(commandLine, isLongOpt = true)
    val shortOptionsVariants = completeOptions(commandLine, isLongOpt = false)
    val completion = when {
      commandLine.toComplete.startsWith("--") ->
        taskOptionsVariants + longOptionsVariants + shortOptionsVariants + taskClassArgumentsVariants + tasksVariants
      commandLine.toComplete.startsWith("-") ->
        taskOptionsVariants + shortOptionsVariants + longOptionsVariants + taskClassArgumentsVariants + tasksVariants
      commandLine.toComplete.startsWith(":") ->
        tasksVariants + taskOptionsVariants + shortOptionsVariants + longOptionsVariants + taskClassArgumentsVariants
      else ->
        taskClassArgumentsVariants + tasksVariants + taskOptionsVariants + longOptionsVariants + shortOptionsVariants
    }
    val prefix = commandLine.prefix
    return completion.map { if (prefix.isEmpty()) "$helpCommand $it" else "$helpCommand $prefix $it" }
  }

  override fun execute(dataContext: DataContext, value: String) {
    val commandLine = parseCommandLine(value)!!
    val context = createContext(dataContext)
    if (context.externalProjectPath == null) {
      Messages.showWarningDialog(
        context.project,
        IdeBundle.message("run.anything.notification.warning.content", commandLine.command),
        IdeBundle.message("run.anything.notification.warning.title"))
      return
    }
    val executor = EXECUTOR_KEY.getData(dataContext)
    GradleExecuteTaskAction.runGradle(context.project, executor, context.externalProjectPath, commandLine.command)
  }

  private fun completeTasks(commandLine: CommandLine, context: Context): List<String> {
    return getTasks(context)
      .filterNot { commandLine.commands.any { task -> matchTask(task, it.first, it.second) } }
      .flatMap {
        when {
          it.second.isInherited -> sequenceOf(it.first.removePrefix(":"))
          else -> sequenceOf(it.first, it.first.removePrefix(":"))
        }
      }
      .toList()
  }

  private fun completeOptions(commandLine: CommandLine, isLongOpt: Boolean): List<String> {
    return GradleCommandLineOptionsProvider.getSupportedOptions().options
      .filterIsInstance<Option>()
      .mapNotNull { if (isLongOpt) it.longOpt else it.opt }
      .map { if (isLongOpt) "--$it" else "-$it" }
      .filter { it !in commandLine.commands }
  }

  private fun completeTaskOptions(commandLine: CommandLine, context: Context): List<String> {
    val task = commandLine.commands.lastOrNull() ?: return emptyList()
    return getTaskOptions(context, task).map { it.name }
  }

  private fun completeTaskClassArguments(commandLine: CommandLine, context: Context): List<String> {
    if (commandLine.commands.size < 2) return emptyList()
    val task = commandLine.commands[commandLine.commands.size - 2]
    val optionName = commandLine.commands[commandLine.commands.size - 1]
    val options = getTaskOptions(context, task)
    val option = ContainerUtil.find(options) { optionName == it.name } ?: return emptyList()
    if (!option.argumentTypes.contains(TaskOption.ArgumentType.CLASS)) return emptyList()
    val callChain = when {
      !commandLine.toComplete.contains(".") -> "*"
      else -> substringBeforeLast(commandLine.toComplete, ".") + "."
    }
    val result = ArrayList<String>()
    val model = GotoClassModel2(context.project)
    val parameters = FindSymbolParameters.simple(context.project, false)
    model.processNames({ result.add("$callChain$it") }, parameters)
    return result
  }

  private fun getTaskOptions(context: Context, task: String): List<TaskOption> {
    val provider = GradleCommandLineTaskOptionsProvider()
    return getTasks(context)
      .filter { matchTask(task, it.first, it.second) }
      .flatMap { provider.getTaskOptions(it.second).asSequence() }
      .toList()
  }

  private fun matchTask(name: String, fqName: String, taskData: TaskData): Boolean {
    return fqName == name ||
           fqName.removePrefix(":") == name ||
           taskData.isInherited && fqName.split(":").last() == name
  }

  private fun getTasks(context: Context): Sequence<Pair<String, TaskData>> {
    val gradlePath = context.gradlePath?.removeSuffix(":") ?: return emptySequence()
    return sequence {
      for ((path, value) in context.tasks.entrySet()) {
        val taskPath = path.removeSuffix(":")
        for (taskData in value) {
          val taskName = taskData.name.removePrefix(path).removePrefix(":")
          val taskFqn = "$taskPath:$taskName".removePrefix(gradlePath)
          yield(taskFqn to taskData)
        }
      }
    }
  }

  private fun fetchTasks(project: Project): Map<String, MultiMap<String, TaskData>> {
    return CachedValuesManager.getManager(project).getCachedValue(project) {
      CachedValueProvider.Result.create(getTasksMap(project), ProjectRootManager.getInstance(project))
    }
  }

  private fun getTasksMap(project: Project): Map<String, MultiMap<String, TaskData>> {
    val tasks = LinkedHashMap<String, MultiMap<String, TaskData>>()
    val projectDataManager = ProjectDataManager.getInstance()
    val projects = MultiMap.createOrderedSet<String, DataNode<ModuleData>>()
    for (settings in GradleSettings.getInstance(project).linkedProjectsSettings) {
      val projectInfo = projectDataManager.getExternalProjectData(project, SYSTEM_ID, settings.externalProjectPath)
      val compositeParticipants = settings.compositeBuild?.compositeParticipants ?: emptyList()
      val compositeProjects = compositeParticipants.flatMap { it.projects.map { module -> module to it.rootPath } }.toMap()
      val projectNode = projectInfo?.externalProjectStructure ?: continue
      val moduleNodes = getChildren(projectNode, ProjectKeys.MODULE)
      for (moduleNode in moduleNodes) {
        val externalModulePath = moduleNode.data.linkedExternalProjectPath
        val projectPath = compositeProjects[externalModulePath] ?: settings.externalProjectPath
        projects.putValue(projectPath, moduleNode)
      }
    }
    for ((_, moduleNodes) in projects.entrySet()) {
      val projectTasks = MultiMap.createOrderedSet<String, TaskData>()
      val modulePaths = PathPrefixTreeMap<String>(":", removeTrailingSeparator = false)
      for (moduleNode in moduleNodes) {
        val moduleData = moduleNode.data
        val gradlePath = getGradlePath(moduleData)
        modulePaths[gradlePath] = moduleData.linkedExternalProjectPath
        for (taskNode in getChildren(moduleNode, ProjectKeys.TASK)) {
          val taskData = taskNode.data
          val taskName = taskData.name
          if (isNotEmpty(taskName)) {
            projectTasks.putValue(gradlePath, taskData)
          }
        }
      }
      for ((gradlePath, modulePath) in modulePaths) {
        val moduleTasks = MultiMap.createOrderedSet<String, TaskData>()
        val descendantPaths = modulePaths.getAllDescendantKeys(gradlePath)
        for (descendantPath in descendantPaths) {
          moduleTasks.putValues(descendantPath, projectTasks.get(descendantPath))
        }
        tasks[modulePath] = moduleTasks
      }
    }
    return tasks
  }

  private fun parseCommandLine(commandLine: String): CommandLine? {
    val command = when {
      commandLine.startsWith(helpCommand) -> trimStart(commandLine, helpCommand)
      helpCommand.startsWith(commandLine) -> ""
      else -> return null
    }
    val prefix = notNullize(substringBeforeLast(command, " "), "").trim()
    val toComplete = notNullize(substringAfterLast(command, " "), "").trim()
    val commands = ParametersListUtil.parse(prefix)
    return CommandLine(commands, command, prefix, toComplete)
  }

  private fun createContext(dataContext: DataContext): Context {
    val project = fetchProject(dataContext)
    val context = dataContext.getData(RunAnythingProvider.EXECUTING_CONTEXT) ?: ProjectContext(project)
    val externalProjectPath = context.getProjectPath()
    val gradlePath = context.getGradlePath(project)
    val tasks = fetchTasks(project)[externalProjectPath] ?: MultiMap()
    return Context(context, project, gradlePath, externalProjectPath, tasks)
  }

  private fun RunAnythingContext.getProjectPath() = when (this) {
    is ProjectContext ->
      GradleSettings.getInstance(project).linkedProjectsSettings.firstOrNull()
        ?.let { findProjectData(project, SYSTEM_ID, it.externalProjectPath) }
        ?.data?.linkedExternalProjectPath
    is ModuleContext -> ExternalSystemApiUtil.getExternalProjectPath(module)
    is RecentDirectoryContext -> path
    is BrowseRecentDirectoryContext -> null
  }

  private fun RunAnythingContext.getGradlePath(project: Project) = when (this) {
    is ProjectContext -> ":"
    is ModuleContext -> getGradlePath(module)
    is RecentDirectoryContext -> GradleUtil.findGradleModuleData(project, path)
      ?.let { getGradlePath(it.data) }
    is BrowseRecentDirectoryContext -> null
  }

  private fun Module.isSourceRoot(): Boolean {
    return GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY == ExternalSystemApiUtil.getExternalModuleType(this)
  }

  private fun getGradlePath(module: Module) =
    GradleProjectResolverUtil.getGradlePath(module)
      ?.removeSuffix(":")

  private fun getGradlePath(module: ModuleData) =
    GradleProjectResolverUtil.getGradlePath(module)
      .removeSuffix(":")

  private data class CommandLine(
    val commands: List<String>,
    val command: String,
    val prefix: String,
    val toComplete: String
  )

  private data class Context(
    val context: RunAnythingContext,
    val project: Project,
    val gradlePath: String?,
    val externalProjectPath: String?,
    val tasks: MultiMap<String, TaskData>
  )

  companion object {
    const val HELP_COMMAND = "gradle"
  }
}
