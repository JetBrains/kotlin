// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.Executor;
import com.intellij.ide.actions.runAnything.activity.RunAnythingProviderBase;
import com.intellij.ide.actions.runAnything.items.RunAnythingHelpItem;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.ide.util.gotoByName.ChooseByNameModelEx;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.indexing.FindSymbolParameters;
import groovyjarjarcommonscli.Option;
import icons.GradleIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.action.GradleExecuteTaskAction;
import org.jetbrains.plugins.gradle.service.execution.cmd.GradleCommandLineOptionsProvider;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.util.*;

import static com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY;
import static com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getChildren;
import static com.intellij.openapi.util.text.StringUtil.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleRunAnythingProvider extends RunAnythingProviderBase<String> {

  public static final String HELP_COMMAND = "gradle";

  @NotNull
  @Override
  public RunAnythingItem getMainListItem(@NotNull DataContext dataContext, @NotNull String value) {
    return new RunAnythingGradleItem(getCommand(value), getIcon(value));
  }

  @Nullable
  @Override
  public String findMatchingValue(@NotNull DataContext dataContext, @NotNull String pattern) {
    return pattern.startsWith(getHelpCommand()) ? getCommand(pattern) : null;
  }

  @NotNull
  @Override
  public Collection<String> getValues(@NotNull DataContext dataContext, @NotNull String pattern) {
    CommandLineInfo commandLine = parseCommandLine(pattern);
    if (commandLine == null) return Collections.emptyList();

    List<String> result = ContainerUtil.newSmartList();

    appendProjectsVariants(result, dataContext, commandLine.prefix);
    if (!result.isEmpty()) return result;

    processTaskOptionsVariants(dataContext, commandLine, it -> result.add(commandLine.prefix + it));
    if (!result.isEmpty()) return result;

    processTaskClassArgumentsVariants(dataContext, commandLine, it -> result.add(commandLine.prefix + it));
    if (!result.isEmpty()) return result;

    appendArgumentsVariants(result, commandLine.prefix, commandLine.toComplete);
    if (!result.isEmpty()) return result;

    appendTasksVariants(result, commandLine.prefix, dataContext);
    return result;
  }

  @Nullable
  private CommandLineInfo parseCommandLine(@NotNull String commandLine) {
    String helpCommand = getHelpCommand();
    if (helpCommand.startsWith(commandLine)) {
      commandLine = helpCommand;
    }
    else if (!commandLine.startsWith(helpCommand)) {
      return null;
    }
    String prefix = notNullize(substringBeforeLast(commandLine, " "), helpCommand).trim() + ' ';
    String toComplete = notNullize(substringAfterLast(commandLine, " "));
    List<String> commands = ContainerUtil.filter(prefix.trim().split(" "), it -> !it.isEmpty());
    if (commands.isEmpty()) return null;
    if (!commands.get(0).equals(helpCommand)) return null;
    String externalProjectName = commands.size() > 1 ? commands.get(1) : "";
    return new CommandLineInfo(prefix, toComplete, externalProjectName, commands.subList(1, commands.size()));
  }

  private void appendProjectsVariants(@NotNull List<? super String> result,
                                      @NotNull DataContext dataContext,
                                      @NotNull String prefix) {
    if (!prefix.trim().equals(getHelpCommand())) return;

    Project project = fetchProject(dataContext);
    Collection<GradleProjectSettings> projectsSettings = GradleSettings.getInstance(project).getLinkedProjectsSettings();
    // do not show projects variants if only one gradle project is linked
    if (projectsSettings.size() <= 1) return;

    ProjectDataManager dataManager = ProjectDataManager.getInstance();
    projectsSettings.stream()
      .map(setting -> dataManager.getExternalProjectData(project, GradleConstants.SYSTEM_ID, setting.getExternalProjectPath()))
      .filter(projectInfo -> projectInfo != null && projectInfo.getExternalProjectStructure() != null)
      .map(projectInfo -> projectInfo.getExternalProjectStructure().getData())
      .forEach(data -> result.add(prefix + data.getExternalName()));
  }

  private void appendTasksVariants(@NotNull List<? super String> result, @NotNull String prefix, @NotNull DataContext dataContext) {
    Project project = fetchProject(dataContext);
    String commandLine = trimStart(prefix, getHelpCommand()).trim();
    ProjectData projectData = getProjectData(project, commandLine);
    if (projectData == null) return;

    MultiMap<String, TaskData> tasks = fetchTasks(dataContext).get(projectData);
    if (tasks == null) return;

    for (Map.Entry<String, Collection<TaskData>> entry : tasks.entrySet()) {
      for (TaskData taskData : entry.getValue()) {
        String taskName = taskData.getName();
        String taskFqn = entry.getKey() + taskName;
        result.add(prefix + taskFqn);
      }
    }
  }

  private static void appendArgumentsVariants(@NotNull List<? super String> result, @NotNull String prefix, @NotNull String toComplete) {
    if (!toComplete.startsWith("-")) return;

    boolean isLongOpt = toComplete.startsWith("--");
    prefix += (isLongOpt ? "--" : "-");
    for (Object option : GradleCommandLineOptionsProvider.getSupportedOptions().getOptions()) {
      if (option instanceof Option) {
        String opt = isLongOpt ? ((Option)option).getLongOpt() : ((Option)option).getOpt();
        if (isNotEmpty(opt)) {
          result.add(prefix + opt);
        }
      }
    }
  }

  private static void processTaskOptionsVariants(@NotNull DataContext dataContext,
                                                 @NotNull CommandLineInfo commandLineInfo,
                                                 @NotNull Processor<String> processor) {
    if (commandLineInfo.commands.isEmpty()) return;
    String task = commandLineInfo.commands.get(commandLineInfo.commands.size() - 1);
    List<TaskOption> options = getTaskOptions(dataContext, commandLineInfo.externalProjectName, task);
    options.forEach(it -> processor.process(it.getName()));
  }

  private static void processTaskClassArgumentsVariants(@NotNull DataContext dataContext,
                                                        @NotNull CommandLineInfo commandLineInfo,
                                                        @NotNull Processor<String> processor) {
    if (commandLineInfo.commands.size() < 2) return;
    String task = commandLineInfo.commands.get(commandLineInfo.commands.size() - 2);
    String optionName = commandLineInfo.commands.get(commandLineInfo.commands.size() - 1);
    List<TaskOption> options = getTaskOptions(dataContext, commandLineInfo.externalProjectName, task);
    TaskOption option = ContainerUtil.find(options, it -> optionName.equals(it.getName()));
    if (option == null) return;
    if (!option.getArgumentTypes().contains(TaskOption.ArgumentType.CLASS)) return;
    String toComplete = commandLineInfo.toComplete;
    String callChain = toComplete.isEmpty() || !toComplete.contains(".") ? "*" : substringBeforeLast(toComplete, ".");
    Project project = fetchProject(dataContext);
    ChooseByNameModelEx model = new GotoClassModel2(project);
    model.processNames(it -> processor.process(callChain + "." + it), FindSymbolParameters.simple(project, false));
  }

  private static List<TaskOption> getTaskOptions(@NotNull DataContext dataContext,
                                                 @NotNull String externalProjectName,
                                                 @NotNull String task) {
    Project project = fetchProject(dataContext);
    ProjectData projectData = getProjectData(project, externalProjectName);
    if (projectData == null) return Collections.emptyList();
    MultiMap<String, TaskData> tasks = fetchTasks(dataContext).get(projectData);
    if (tasks == null) return Collections.emptyList();
    GradleCommandLineTaskOptionsProvider provider = new GradleCommandLineTaskOptionsProvider();
    for (Map.Entry<String, Collection<TaskData>> entry : tasks.entrySet()) {
      for (TaskData taskData : entry.getValue()) {
        String taskName = taskData.getName();
        String taskFqn = entry.getKey() + taskName;
        if (!taskFqn.equals(task)) continue;
        return provider.getTaskOptions(taskData);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public void execute(@NotNull DataContext dataContext, @NotNull String value) {
    Project project = fetchProject(dataContext);
    String commandLine = trimStart(value, getHelpCommand()).trim();

    ProjectData projectData = getProjectData(project, commandLine);
    if (projectData == null) return;

    commandLine = trimStart(commandLine, projectData.getExternalName());
    Executor executor = EXECUTOR_KEY.getData(dataContext);
    GradleExecuteTaskAction.runGradle(project, executor, projectData.getLinkedExternalProjectPath(), commandLine);
  }

  @Nullable
  private static ProjectData getProjectData(@NotNull Project project, @NotNull String commandLine) {
    Collection<GradleProjectSettings> projectsSettings = GradleSettings.getInstance(project).getLinkedProjectsSettings();
    if (projectsSettings.isEmpty()) return null;

    ProjectDataManager dataManager = ProjectDataManager.getInstance();
    return projectsSettings.stream()
      .map(setting -> dataManager.getExternalProjectData(project, GradleConstants.SYSTEM_ID, setting.getExternalProjectPath()))
      .filter(projectInfo -> projectInfo != null && projectInfo.getExternalProjectStructure() != null)
      .map(projectInfo -> projectInfo.getExternalProjectStructure().getData())
      .filter(projectData -> projectsSettings.size() == 1 || startsWith(commandLine, projectData.getExternalName()))
      .findFirst().orElse(null);
  }

  @NotNull
  @Override
  public String getCommand(@NotNull String value) {
    return value;
  }

  @Nullable
  @Override
  public Icon getIcon(@NotNull String value) {
    return GradleIcons.Gradle;
  }

  @Nullable
  @Override
  public RunAnythingHelpItem getHelpItem(@NotNull DataContext dataContext) {
    String placeholder = getHelpCommandPlaceholder(dataContext);
    String commandPrefix = getHelpCommand();
    return new RunAnythingHelpItem(placeholder, commandPrefix, getHelpDescription(), getHelpIcon());
  }

  @Override
  @NotNull
  public String getCompletionGroupTitle() {
    return "Gradle tasks";
  }

  @NotNull
  @Override
  public String getHelpCommandPlaceholder() {
    return getHelpCommandPlaceholder(null);
  }

  @NotNull
  public String getHelpCommandPlaceholder(@Nullable DataContext dataContext) {
    if (dataContext != null) {
      Project project = fetchProject(dataContext);
      if (GradleSettings.getInstance(project).getLinkedProjectsSettings().size() > 1) {
        return "gradle <rootProjectName> <taskName...> <--option-name...>";
      }
    }
    return "gradle <taskName...> <--option-name...>";
  }

  @NotNull
  @Override
  public String getHelpCommand() {
    return HELP_COMMAND;
  }

  @Override
  public Icon getHelpIcon() {
    return GradleIcons.Gradle;
  }

  private static Map<ProjectData, MultiMap<String, TaskData>> fetchTasks(@NotNull DataContext dataContext) {
    Project project = fetchProject(dataContext);
    return CachedValuesManager.getManager(project).getCachedValue(
      project, () -> CachedValueProvider.Result.create(getTasksMap(project), ProjectRootManager.getInstance(project)));
  }

  @NotNull
  private static Map<ProjectData, MultiMap<String, TaskData>> getTasksMap(Project project) {
    Map<ProjectData, MultiMap<String, TaskData>> tasks = new LinkedHashMap<>();
    for (GradleProjectSettings setting : GradleSettings.getInstance(project).getLinkedProjectsSettings()) {
      final ExternalProjectInfo projectData =
        ProjectDataManager.getInstance().getExternalProjectData(project, GradleConstants.SYSTEM_ID, setting.getExternalProjectPath());
      if (projectData == null || projectData.getExternalProjectStructure() == null) continue;

      MultiMap<String, TaskData> projectTasks = MultiMap.createOrderedSet();
      for (DataNode<ModuleData> moduleDataNode : getChildren(projectData.getExternalProjectStructure(), ProjectKeys.MODULE)) {
        String gradlePath = GradleProjectResolverUtil.getGradlePath(moduleDataNode.getData());
        for (DataNode<TaskData> node : getChildren(moduleDataNode, ProjectKeys.TASK)) {
          TaskData taskData = node.getData();
          String taskName = taskData.getName();
          if (isNotEmpty(taskName)) {
            String taskPathPrefix = ":".equals(gradlePath) || taskName.startsWith(gradlePath) ? "" : (gradlePath + ':');
            projectTasks.putValue(taskPathPrefix, taskData);
          }
        }
      }
      tasks.put(projectData.getExternalProjectStructure().getData(), projectTasks);
    }
    return tasks;
  }

  private static class CommandLineInfo {
    final String prefix;
    final String toComplete;
    final String externalProjectName;
    final List<String> commands;

    private CommandLineInfo(String prefix, String toComplete, String externalProjectName, List<String> commands) {
      this.prefix = prefix;
      this.toComplete = toComplete;
      this.externalProjectName = externalProjectName;
      this.commands = commands;
    }
  }
}
