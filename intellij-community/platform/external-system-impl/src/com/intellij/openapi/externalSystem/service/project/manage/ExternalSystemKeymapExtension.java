// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.action.ExternalSystemAction;
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.ui.SelectExternalTaskDialog;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.keymap.KeymapExtension;
import com.intellij.openapi.keymap.KeymapGroup;
import com.intellij.openapi.keymap.KeymapGroupFactory;
import com.intellij.openapi.keymap.impl.ui.*;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashSet;
import icons.ExternalSystemIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemKeymapExtension implements KeymapExtension {

  @FunctionalInterface
  public interface ActionsProvider {
    ExtensionPointName<ActionsProvider> EP_NAME = ExtensionPointName.create("com.intellij.externalSystemKeymapProvider");

    KeymapGroup createGroup(Condition<? super AnAction> condition, final Project project);
  }


  @Override
  public KeymapGroup createGroup(Condition<AnAction> condition, final Project project) {
    KeymapGroup result = KeymapGroupFactory.getInstance().createGroup(
      ExternalSystemBundle.message("external.system.keymap.group"), ExternalSystemIcons.TaskGroup);

    AnAction[] externalSystemActions = ActionsTreeUtil.getActions("ExternalSystem.Actions");
    for (AnAction action : externalSystemActions) {
      ActionsTreeUtil.addAction(result, action, condition);
    }

    if (project == null) return result;

    MultiMap<ProjectSystemId, String> projectToActionsMapping = MultiMap.create();
    for (ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      projectToActionsMapping.putValues(manager.getSystemId(), ContainerUtil.emptyList());
    }

    ActionManager actionManager = ActionManager.getInstance();
    if (actionManager != null) {
      for (String eachId : actionManager.getActionIds(getActionPrefix(project, null))) {
        AnAction eachAction = actionManager.getAction(eachId);

        if (!(eachAction instanceof MyExternalSystemAction)) continue;
        if (condition != null && !condition.value(actionManager.getActionOrStub(eachId))) continue;

        MyExternalSystemAction taskAction = (MyExternalSystemAction)eachAction;
        projectToActionsMapping.putValue(taskAction.getSystemId(), eachId);
      }
    }

    Map<ProjectSystemId, KeymapGroup> keymapGroupMap = new HashMap<>();
    for (ProjectSystemId systemId : projectToActionsMapping.keySet()) {
      if (!keymapGroupMap.containsKey(systemId)) {
        final Icon projectIcon = ExternalSystemUiUtil.getUiAware(systemId).getProjectIcon();
        KeymapGroup group = KeymapGroupFactory.getInstance().createGroup(systemId.getReadableName(), projectIcon);
        keymapGroupMap.put(systemId, group);
      }
    }

    for (Map.Entry<ProjectSystemId, Collection<String>> each : projectToActionsMapping.entrySet()) {
      Collection<String> tasks = each.getValue();
      final ProjectSystemId systemId = each.getKey();
      final KeymapGroup systemGroup = keymapGroupMap.get(systemId);
      if (systemGroup == null) continue;
      for (String actionId : tasks) {
        systemGroup.addActionId(actionId);
      }
      if (systemGroup instanceof Group) {
        Icon icon = AllIcons.General.Add;
        ((Group)systemGroup).addHyperlink(new Hyperlink(icon, "Choose a task to assign a shortcut") {
          @Override
          public void onClick(MouseEvent e) {
            SelectExternalTaskDialog dialog = new SelectExternalTaskDialog(systemId, project);
            if (dialog.showAndGet() && dialog.getResult() != null) {
              TaskData taskData = dialog.getResult().second;
              String ownerModuleName = dialog.getResult().first;
              ExternalSystemTaskAction externalSystemAction =
                (ExternalSystemTaskAction)getOrRegisterAction(project, ownerModuleName, taskData);

              ApplicationManager.getApplication().getMessageBus().syncPublisher(KeymapListener.CHANGE_TOPIC).processCurrentKeymapChanged();

              Settings allSettings = Settings.KEY.getData(DataManager.getInstance().getDataContext(e.getComponent()));
              KeymapPanel keymapPanel = allSettings != null ? allSettings.find(KeymapPanel.class) : null;
              if (keymapPanel != null) {
                // clear actions filter
                keymapPanel.showOption("");
                keymapPanel.selectAction(externalSystemAction.myId);
              }
            }
          }
        });
      }
    }

    for (KeymapGroup keymapGroup : keymapGroupMap.values()) {
      if (isGroupFiltered(condition, keymapGroup)) {
        result.addGroup(keymapGroup);
      }
    }

    for (ActionsProvider extension : ActionsProvider.EP_NAME.getExtensions()) {
      KeymapGroup keymapGroup = extension.createGroup(condition, project);
      if (isGroupFiltered(condition, keymapGroup)) {
        result.addGroup(keymapGroup);
      }
    }

    return result;
  }

  public static void updateActions(Project project, @NotNull Collection<? extends DataNode<TaskData>> taskData) {
    clearActions(project, taskData);
    createActions(project, taskData);
  }

  public static ExternalSystemAction getOrRegisterAction(Project project, String group, TaskData taskData) {
    ExternalSystemTaskAction action = new ExternalSystemTaskAction(project, group, taskData);
    ActionManager manager = ActionManager.getInstance();
    AnAction anAction = manager.getAction(action.getId());
    if (anAction instanceof ExternalSystemTaskAction && action.equals(anAction)) {
      return (ExternalSystemAction)anAction;
    }
    manager.replaceAction(action.getId(), action);
    return action;
  }

  private static boolean isGroupFiltered(Condition<? super AnAction> condition, KeymapGroup keymapGroup) {
    final EmptyAction emptyAction = new EmptyAction();
    if (condition != null && !condition.value(emptyAction) && keymapGroup instanceof Group) {
      final Group group = (Group)keymapGroup;
      return group.getSize() > 1 || condition.value(new EmptyAction(group.getName(), null, null));
    }
    return true;
  }

  private static void createActions(Project project, Collection<? extends DataNode<TaskData>> taskNodes) {
    ActionManager actionManager = ActionManager.getInstance();
    final ExternalSystemShortcutsManager shortcutsManager = ExternalProjectsManagerImpl.getInstance(project).getShortcutsManager();
    if (actionManager != null) {
      for (DataNode<TaskData> each : taskNodes) {
        final DataNode<ModuleData> moduleData = ExternalSystemApiUtil.findParent(each, ProjectKeys.MODULE);
        if (moduleData == null || moduleData.isIgnored()) continue;
        TaskData taskData = each.getData();
        ExternalSystemTaskAction eachAction = new ExternalSystemTaskAction(project, moduleData.getData().getInternalName(), taskData);
        if (shortcutsManager.hasShortcuts(taskData.getLinkedExternalProjectPath(), taskData.getName())) {
          actionManager.replaceAction(eachAction.getId(), eachAction);
        }
        else {
          actionManager.unregisterAction(eachAction.getId());
        }
      }
    }
  }

  static void clearActions(@NotNull ExternalSystemShortcutsManager externalSystemShortcutsManager) {
    ActionManager manager = ActionManager.getInstance();
    if (manager != null) {
      for (String each : manager.getActionIds(getActionPrefix(externalSystemShortcutsManager, null))) {
        manager.unregisterAction(each);
      }
    }
  }

  private static void clearActions(Project project, Collection<? extends DataNode<TaskData>> taskData) {
    ActionManager actionManager = ActionManager.getInstance();
    if (actionManager != null) {
      Set<String> externalProjectPaths = new HashSet<>();
      for (DataNode<TaskData> node : taskData) {
        externalProjectPaths.add(node.getData().getLinkedExternalProjectPath());
      }

      for (String externalProjectPath : externalProjectPaths) {
        for (String eachAction : actionManager.getActionIds(getActionPrefix(project, externalProjectPath))) {
          AnAction action = actionManager.getAction(eachAction);
          if (!(action instanceof ExternalSystemRunConfigurationAction)) {
            actionManager.unregisterAction(eachAction);
          }
        }
      }
    }
  }

  @NotNull
  public static String getActionPrefix(@NotNull Project project, @Nullable String path) {
    ExternalSystemShortcutsManager externalSystemShortcutsManager = ExternalProjectsManagerImpl.getInstance(project).getShortcutsManager();
    return getActionPrefix(externalSystemShortcutsManager, path);
  }

  @NotNull
  private static String getActionPrefix(@NotNull ExternalSystemShortcutsManager externalSystemShortcutsManager, @Nullable String path) {
    return externalSystemShortcutsManager.getActionId(path, null);
  }

  static void updateRunConfigurationActions(Project project, ProjectSystemId systemId) {
    final AbstractExternalSystemTaskConfigurationType configurationType = ExternalSystemUtil.findConfigurationType(systemId);
    if (configurationType == null) return;

    ActionManager actionManager = ActionManager.getInstance();
    for (String eachAction : actionManager.getActionIds(getActionPrefix(project, null))) {
      AnAction action = actionManager.getAction(eachAction);
      if (action instanceof ExternalSystemRunConfigurationAction) {
        actionManager.unregisterAction(eachAction);
      }
    }

    Set<RunnerAndConfigurationSettings> settings = new THashSet<>(
      RunManager.getInstance(project).getConfigurationSettingsList(configurationType));

    final ExternalSystemShortcutsManager shortcutsManager = ExternalProjectsManagerImpl.getInstance(project).getShortcutsManager();
    for (RunnerAndConfigurationSettings configurationSettings : settings) {
      ExternalSystemRunConfigurationAction runConfigurationAction =
        new ExternalSystemRunConfigurationAction(project, configurationSettings);
      String id = runConfigurationAction.getId();
      if (shortcutsManager.hasShortcuts(id)) {
        actionManager.replaceAction(id, runConfigurationAction);
      }
      else {
        actionManager.unregisterAction(id);
      }
    }
  }

  public static ExternalSystemAction getOrRegisterAction(Project project, RunnerAndConfigurationSettings configurationSettings) {
    ActionManager manager = ActionManager.getInstance();
    ExternalSystemRunConfigurationAction runConfigurationAction =
      new ExternalSystemRunConfigurationAction(project, configurationSettings);
    String id = runConfigurationAction.getId();
    manager.replaceAction(id, runConfigurationAction);
    return runConfigurationAction;
  }

  private abstract static class MyExternalSystemAction extends ExternalSystemAction {
    public abstract String getId();

    public abstract String getGroup();

    public abstract ProjectSystemId getSystemId();
  }

  private static class ExternalSystemTaskAction extends MyExternalSystemAction {
    private final String myId;
    private final String myGroup;
    private final TaskData myTaskData;

    ExternalSystemTaskAction(Project project, String group, TaskData taskData) {
      myGroup = group;
      myTaskData = taskData;
      myId = getActionPrefix(project, taskData.getLinkedExternalProjectPath()) + taskData.getName();

      Presentation template = getTemplatePresentation();
      template.setText(myTaskData.getName() + " (" + group + ")", false);
      template.setDescription(myTaskData.getOwner().getReadableName() + " task action");
      template.setIcon(ExternalSystemIcons.Task);
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e) {
      return hasProject(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final ExternalTaskExecutionInfo taskExecutionInfo = ExternalSystemActionUtil.buildTaskInfo(myTaskData);
      ExternalSystemUtil.runTask(
        taskExecutionInfo.getSettings(), taskExecutionInfo.getExecutorId(), getProject(e), myTaskData.getOwner());
    }

    public TaskData getTaskData() {
      return myTaskData;
    }

    @Override
    public String toString() {
      return myTaskData.toString();
    }

    @Override
    public String getGroup() {
      return myGroup;
    }

    @Override
    public ProjectSystemId getSystemId() {
      return myTaskData.getOwner();
    }

    @Override
    public String getId() {
      return myId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ExternalSystemTaskAction)) return false;

      ExternalSystemTaskAction action = (ExternalSystemTaskAction)o;

      if (myId != null ? !myId.equals(action.myId) : action.myId != null) return false;
      if (myGroup != null ? !myGroup.equals(action.myGroup) : action.myGroup != null) return false;
      if (!myTaskData.equals(action.myTaskData)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = myId != null ? myId.hashCode() : 0;
      result = 31 * result + (myGroup != null ? myGroup.hashCode() : 0);
      result = 31 * result + myTaskData.hashCode();
      return result;
    }
  }

  private static class ExternalSystemRunConfigurationAction extends MyExternalSystemAction {
    private final String myId;
    private final String myGroup;
    private final RunnerAndConfigurationSettings myConfigurationSettings;
    private final ProjectSystemId systemId;

    ExternalSystemRunConfigurationAction(Project project, RunnerAndConfigurationSettings configurationSettings) {
      myConfigurationSettings = configurationSettings;
      ExternalSystemRunConfiguration runConfiguration = (ExternalSystemRunConfiguration)configurationSettings.getConfiguration();
      systemId = runConfiguration.getSettings().getExternalSystemId();

      ExternalSystemUiAware uiAware = ExternalSystemUiUtil.getUiAware(systemId);
      myGroup = uiAware.getProjectRepresentationName(runConfiguration.getSettings().getExternalProjectPath(), null);
      String actionIdPrefix = getActionPrefix(project, runConfiguration.getSettings().getExternalProjectPath());
      myId = actionIdPrefix + configurationSettings.getName();

      Presentation template = getTemplatePresentation();
      template.setText(myConfigurationSettings.getName(), false);
      template.setIcon(runConfiguration.getIcon());
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e) {
      return hasProject(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      ProgramRunnerUtil.executeConfiguration(myConfigurationSettings, DefaultRunExecutor.getRunExecutorInstance());
    }

    @Override
    public String toString() {
      return myConfigurationSettings.toString();
    }

    @Override
    public String getGroup() {
      return myGroup;
    }

    @Override
    public ProjectSystemId getSystemId() {
      return systemId;
    }

    @Override
    public String getId() {
      return myId;
    }
  }
}

