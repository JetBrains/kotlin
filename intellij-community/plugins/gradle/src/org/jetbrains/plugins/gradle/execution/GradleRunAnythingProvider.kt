// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import com.intellij.execution.Executor
import com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY
import com.intellij.ide.actions.runAnything.RunAnythingContext
import com.intellij.ide.actions.runAnything.RunAnythingContext.*
import com.intellij.ide.actions.runAnything.RunAnythingUtil
import com.intellij.ide.actions.runAnything.activity.RunAnythingCommandLineProvider
import com.intellij.ide.actions.runAnything.getPath
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
import com.intellij.openapi.util.text.StringUtil.isNotEmpty
import com.intellij.openapi.util.text.StringUtil.substringBeforeLast
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap
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


class GradleRunAnythingProvider : RunAnythingCommandLineProvider() {
  override fun getIcon(value: String): Icon? = GradleIcons.Gradle

  override fun getHelpGroupTitle() = "Gradle"

  override fun getCompletionGroupTitle() = "Gradle tasks"

  override fun getHelpCommandPlaceholder() = "gradle <taskName...> <--option-name...>"

  override fun getHelpCommand() = HELP_COMMAND

  override fun getHelpCommandAliases() = listOf(SECONDARY_HELP_COMMAND)

  override fun getHelpIcon(): Icon? = GradleIcons.Gradle

  override fun getMainListItem(dataContext: DataContext, value: String) =
    RunAnythingGradleItem(getCommand(value), getIcon(value))

  override fun getExecutionContexts(dataContext: DataContext): List<RunAnythingContext> {
    return super.getExecutionContexts(dataContext).filter {
      it !is ModuleContext || !it.module.isSourceRoot()
    }
  }

  override fun suggestCompletionVariants(dataContext: DataContext, commandLine: CommandLine): Sequence<String> {
    val project = RunAnythingUtil.fetchProject(dataContext)
    val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: ProjectContext(project)
    val context = createContext(project, executionContext, dataContext)
    val (tasksVariants, wildcardTaskVariants) = completeTasks(commandLine, context)
      .partition { it.startsWith(":") }
      .let { it.first.sorted().asSequence() to it.second.sorted().asSequence() }
    val taskOptionsVariants = completeTaskOptions(commandLine, context).sorted()
    val taskClassArgumentsVariants = completeTaskClassArguments(commandLine, context).sorted()
    val longOptionsVariants = completeOptions(commandLine, isLongOpt = true).sorted()
    val shortOptionsVariants = completeOptions(commandLine, isLongOpt = false).sorted()
    return when {
      commandLine.toComplete.startsWith("--") ->
        taskOptionsVariants + longOptionsVariants + shortOptionsVariants + taskClassArgumentsVariants + wildcardTaskVariants + tasksVariants
      commandLine.toComplete.startsWith("-") ->
        taskOptionsVariants + shortOptionsVariants + longOptionsVariants + taskClassArgumentsVariants + wildcardTaskVariants + tasksVariants
      commandLine.toComplete.startsWith(":") ->
        tasksVariants + wildcardTaskVariants + taskOptionsVariants + shortOptionsVariants + longOptionsVariants + taskClassArgumentsVariants
      else ->
        taskClassArgumentsVariants + wildcardTaskVariants + tasksVariants + taskOptionsVariants + longOptionsVariants + shortOptionsVariants
    }
  }

  override fun run(dataContext: DataContext, commandLine: CommandLine): Boolean {
    val project = RunAnythingUtil.fetchProject(dataContext)
    val executionContext = dataContext.getData(EXECUTING_CONTEXT) ?: ProjectContext(project)
    val context = createContext(project, executionContext, dataContext)
    val workDirectory = context.externalProjectPath ?: executionContext.getPath() ?: return false
    GradleExecuteTaskAction.runGradle(project, context.executor, workDirectory, commandLine.command)
    return true
  }

  private fun completeTasks(commandLine: CommandLine, context: Context): Sequence<String> {
    return getTasks(context)
      .filterNot { commandLine.completedParameters.any { task -> matchTask(task, it.first, it.second) } }
      .flatMap {
        when {
          it.second.isInherited -> sequenceOf(it.first.removePrefix(":"))
          else -> sequenceOf(it.first, it.first.removePrefix(":"))
        }
      }
  }

  private fun completeOptions(commandLine: CommandLine, isLongOpt: Boolean): Sequence<String> {
    return GradleCommandLineOptionsProvider.getSupportedOptions().options.asSequence()
      .filterIsInstance<Option>()
      .mapNotNull { if (isLongOpt) it.longOpt else it.opt }
      .map { if (isLongOpt) "--$it" else "-$it" }
      .filter { it !in commandLine }
  }

  private fun completeTaskOptions(commandLine: CommandLine, context: Context): Sequence<String> {
    val task = commandLine.completedParameters.lastOrNull() ?: return emptySequence()
    return getTaskOptions(context, task).map { it.name }
  }

  private fun completeTaskClassArguments(commandLine: CommandLine, context: Context): Sequence<String> {
    if (commandLine.completedParameters.size < 2) return emptySequence()
    val task = commandLine.completedParameters[commandLine.completedParameters.size - 2]
    val optionName = commandLine.completedParameters[commandLine.completedParameters.size - 1]
    val options = getTaskOptions(context, task)
    val option = options.find { optionName == it.name } ?: return emptySequence()
    if (!option.argumentTypes.contains(TaskOption.ArgumentType.CLASS)) return emptySequence()
    val callChain = when {
      !commandLine.toComplete.contains(".") -> "*"
      else -> substringBeforeLast(commandLine.toComplete, ".") + "."
    }
    val result = ArrayList<String>()
    val model = GotoClassModel2(context.project)
    val parameters = FindSymbolParameters.simple(context.project, false)
    model.processNames({ result.add("$callChain$it") }, parameters)
    return result.asSequence()
  }

  private fun getTaskOptions(context: Context, task: String): Sequence<TaskOption> {
    val provider = GradleCommandLineTaskOptionsProvider()
    return getTasks(context)
      .filter { matchTask(task, it.first, it.second) }
      .flatMap { provider.getTaskOptions(it.second).asSequence() }
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

  private fun createContext(project: Project, context: RunAnythingContext, dataContext: DataContext): Context {
    val externalProjectPath = context.getProjectPath()
    val gradlePath = context.getGradlePath(project)
    val tasks = fetchTasks(project)[externalProjectPath] ?: MultiMap()
    val executor = EXECUTOR_KEY.getData(dataContext)
    return Context(context, project, gradlePath, externalProjectPath, tasks, executor)
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

  data class Context(
    val context: RunAnythingContext,
    val project: Project,
    val gradlePath: String?,
    val externalProjectPath: String?,
    val tasks: MultiMap<String, TaskData>,
    val executor: Executor?
  )

  companion object {
    const val HELP_COMMAND = "gradle"
    private const val SECONDARY_HELP_COMMAND = "gradlew"
  }
}
