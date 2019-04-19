/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.service.task.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.ExternalProjectsStateProvider;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator.Phase;
import com.intellij.openapi.externalSystem.service.project.manage.TaskActivationState;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.treeStructure.*;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.SwingHelper;
import icons.ExternalSystemIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.*;

import static com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.getInstance;
import static com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator.TaskActivationEntry;

/**
 * @author Vladislav.Soroka
 */
public class ConfigureTasksActivationDialog extends DialogWrapper {

  @NotNull private final Project myProject;
  @NotNull private final ExternalSystemTaskActivator myTaskActivator;
  @NotNull ProjectSystemId myProjectSystemId;
  private JPanel contentPane;

  private JPanel tasksPanel;
  @SuppressWarnings("unused")
  private JPanel projectFieldPanel;
  private SimpleTree myTree;
  private AbstractTreeBuilder treeBuilder;
  private ComboBox projectCombobox;
  @NotNull
  private final ExternalSystemUiAware uiAware;
  private RootNode myRootNode;

  public ConfigureTasksActivationDialog(@NotNull Project project, @NotNull ProjectSystemId externalSystemId, @NotNull String projectPath) {
    super(project, true);
    myProject = project;
    myProjectSystemId = externalSystemId;
    uiAware = ExternalSystemUiUtil.getUiAware(myProjectSystemId);
    setUpDialog(projectPath);
    setModal(true);
    setTitle(ExternalSystemBundle.message("external.system.task.activation.title", externalSystemId.getReadableName()));
    init();
    myTaskActivator = getInstance(myProject).getTaskActivator();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }

  private void setUpDialog(@NotNull String projectPath) {
    final AbstractExternalSystemSettings externalSystemSettings = ExternalSystemApiUtil.getSettings(myProject, myProjectSystemId);
    //noinspection unchecked
    Collection<ExternalProjectSettings> projectsSettings = externalSystemSettings.getLinkedProjectsSettings();
    List<ProjectItem> projects = ContainerUtil.map(projectsSettings,
                                                   settings -> new ProjectItem(uiAware.getProjectRepresentationName(settings.getExternalProjectPath(), null), settings));

    myTree = new SimpleTree();
    myRootNode = new RootNode();
    treeBuilder = createTreeBuilder(myProject, myRootNode, myTree);
    final ExternalProjectSettings currentProjectSettings = externalSystemSettings.getLinkedProjectSettings(projectPath);
    if (currentProjectSettings != null) {
      SwingHelper.updateItems(projectCombobox, projects,
                              new ProjectItem(uiAware.getProjectRepresentationName(projectPath, null), currentProjectSettings));
    }
    projectCombobox.addActionListener(e -> updateTree(myRootNode));
  }

  private static AbstractTreeBuilder createTreeBuilder(@NotNull Project project, @NotNull SimpleNode root, @NotNull Tree tree) {
    final DefaultTreeModel treeModel = new DefaultTreeModel(new DefaultMutableTreeNode(root));
    tree.setModel(treeModel);
    tree.setRootVisible(false);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    final AbstractTreeBuilder treeBuilder = new AbstractTreeBuilder(tree, treeModel, new SimpleTreeStructure.Impl(root), null) {
      // unique class to simplify search through the logs
    };
    Disposer.register(project, treeBuilder);
    return treeBuilder;
  }

  @Override
  protected JComponent createCenterPanel() {
    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTree).
      setAddAction(button -> {
        ProjectItem projectItem = (ProjectItem)projectCombobox.getSelectedItem();
        if(projectItem == null) return;

        final ExternalProjectInfo projectData = ProjectDataManager.getInstance()
          .getExternalProjectData(myProject, myProjectSystemId, projectItem.myProjectSettings.getExternalProjectPath());

        if (projectData == null || projectData.getExternalProjectStructure() == null) return;

        final List<ProjectPopupItem> popupItems = ContainerUtil.newArrayList();
        for (DataNode<ModuleData> moduleDataNode : ExternalSystemApiUtil
          .findAllRecursively(projectData.getExternalProjectStructure(), ProjectKeys.MODULE)) {
          if(moduleDataNode.isIgnored()) continue;

          final List<String> tasks = ContainerUtil.map(
            ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.TASK), node -> node.getData().getName());
          if (!tasks.isEmpty()) {
            popupItems.add(new ProjectPopupItem(moduleDataNode.getData(), tasks));
          }
        }

        final ChooseProjectStep projectStep = new ChooseProjectStep(popupItems);
        final List<ProjectPopupItem> projectItems = projectStep.getValues();
        ListPopupStep step = projectItems.size() == 1 ? (ListPopupStep)projectStep.onChosen(projectItems.get(0), false) : projectStep;
        assert step != null;
        JBPopupFactory.getInstance().createListPopup(step).show(
          ObjectUtils.notNull(button.getPreferredPopupPoint(), RelativePoint.getSouthEastOf(projectCombobox)));
      }).
      setRemoveAction(button -> {
        List<TaskActivationEntry> tasks = findSelectedTasks();
        myTaskActivator.removeTasks(tasks);
        updateTree(null);
      }).
      setMoveUpAction(button -> moveAction(-1)).
      setMoveUpActionUpdater(e -> isMoveActionEnabled(-1)).
      setMoveDownAction(button -> moveAction(+1)).
      setMoveDownActionUpdater(e -> isMoveActionEnabled(+1)).
      setToolbarPosition(ActionToolbarPosition.RIGHT).
      setToolbarBorder(JBUI.Borders.empty());
    tasksPanel.add(decorator.createPanel());
    return contentPane;
  }

  private boolean isMoveActionEnabled(int increment) {
    final DefaultMutableTreeNode[] selectedNodes = myTree.getSelectedNodes(DefaultMutableTreeNode.class, null);
    if (selectedNodes.length == 0) return false;

    boolean enabled = true;
    for (DefaultMutableTreeNode node : selectedNodes) {
      final DefaultMutableTreeNode sibling = increment == -1 ? node.getPreviousSibling() : node.getNextSibling();
      enabled = enabled && (node.getUserObject() instanceof TaskNode) && sibling != null;
    }
    if (!enabled) {
      enabled = true;
      for (DefaultMutableTreeNode node : selectedNodes) {
        final DefaultMutableTreeNode sibling = increment == -1 ? node.getPreviousSibling() : node.getNextSibling();
        enabled = enabled && (node.getUserObject() instanceof ProjectNode) && sibling != null;
      }
    }
    return enabled;
  }

  private void moveAction(int increment) {
    List<TaskActivationEntry> tasks = findSelectedTasks();
    if (!tasks.isEmpty()) {
      myTaskActivator.moveTasks(tasks, increment);
    }
    else {
      List<String> projectsPaths = findSelectedProjects();
      if (projectsPaths.isEmpty()) return;
      ProjectItem item = (ProjectItem)projectCombobox.getSelectedItem();
      myTaskActivator.moveProjects(myProjectSystemId, projectsPaths, item.myProjectSettings.getModules(), increment);
    }
    moveSelectedRows(myTree, increment);
  }

  private static void moveSelectedRows(@NotNull final SimpleTree tree, final int direction) {
    final TreePath[] selectionPaths = tree.getSelectionPaths();
    if (selectionPaths == null) return;

    ContainerUtil.sort(selectionPaths, new Comparator<TreePath>() {
      @Override
      public int compare(TreePath o1, TreePath o2) {
        return -direction * compare(tree.getRowForPath(o1), tree.getRowForPath(o2));
      }

      private int compare(int x, int y) {
        return Integer.compare(x, y);
      }
    });

    for (TreePath selectionPath : selectionPaths) {
      final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)selectionPath.getLastPathComponent();
      final DefaultMutableTreeNode parent = (DefaultMutableTreeNode)treeNode.getParent();
      final int idx = parent.getIndex(treeNode);
      ((DefaultTreeModel)tree.getModel()).removeNodeFromParent(treeNode);
      ((DefaultTreeModel)tree.getModel()).insertNodeInto(treeNode, parent, idx + direction);
    }

    tree.addSelectionPaths(selectionPaths);
  }

  @NotNull
  private List<TaskActivationEntry> findSelectedTasks() {
    List<TaskActivationEntry> tasks = ContainerUtil.newSmartList();
    for (DefaultMutableTreeNode node : myTree.getSelectedNodes(DefaultMutableTreeNode.class, null)) {
      ContainerUtil.addAll(tasks, findTasksUnder(ContainerUtil.ar((MyNode)node.getUserObject())));
    }
    return tasks;
  }

  @NotNull
  private List<TaskActivationEntry> findTasksUnder(@NotNull SimpleNode[] nodes) {
    List<TaskActivationEntry> tasks = ContainerUtil.newSmartList();
    for (SimpleNode node : nodes) {
      if (node instanceof TaskNode) {
        final TaskNode taskNode = (TaskNode)node;
        final String taskName = taskNode.getName();
        final PhaseNode phaseNode = (PhaseNode)taskNode.getParent();
        tasks.add(new TaskActivationEntry(myProjectSystemId, phaseNode.myPhase, phaseNode.myProjectPath, taskName));
      }
      else {
        ContainerUtil.addAll(tasks, findTasksUnder(node.getChildren()));
      }
    }
    return tasks;
  }

  private List<String> findSelectedProjects() {
    List<String> tasks = ContainerUtil.newArrayList();
    for (DefaultMutableTreeNode node : myTree.getSelectedNodes(DefaultMutableTreeNode.class, null)) {
      if (node.getUserObject() instanceof ProjectNode) {
        final ProjectNode projectNode = (ProjectNode)node.getUserObject();
        tasks.add(projectNode.myProjectPath);
      }
    }
    return tasks;
  }

  private MyNode[] buildProjectsNodes(final ExternalProjectSettings projectSettings,
                                      final ExternalProjectsStateProvider stateProvider,
                                      final RootNode parent) {
    List<String> paths = ContainerUtil.newArrayList(stateProvider.getProjectsTasksActivationMap(myProjectSystemId).keySet());
    paths.retainAll(projectSettings.getModules());

    return ContainerUtil.mapNotNull(ArrayUtil.toStringArray(paths), path -> {
      final MyNode node = new ProjectNode(parent, stateProvider, projectSettings.getExternalProjectPath(), path);
      return node.getChildren().length > 0 ? node : null;
    }, new MyNode[]{});
  }

  private MyNode[] buildProjectPhasesNodes(final String projectPath,
                                           final TaskActivationState tasksActivation,
                                           final MyNode parent) {
    return ContainerUtil.mapNotNull(Phase.values(), phase -> tasksActivation.getTasks(phase).isEmpty() ? null : new PhaseNode(projectPath, phase, tasksActivation, parent), new MyNode[]{});
  }

  private static class ProjectItem {
    private static final int MAX_LENGTH = 80;

    @NotNull String projectName;
    @NotNull ExternalProjectSettings myProjectSettings;

    ProjectItem(@NotNull String projectName, @NotNull ExternalProjectSettings projectPath) {
      this.projectName = projectName;
      this.myProjectSettings = projectPath;
    }

    @Override
    public String toString() {
      return projectName + " (" + truncate(myProjectSettings.getExternalProjectPath()) + ")";
    }

    @NotNull
    private static String truncate(@NotNull String s) {
      return s.length() < MAX_LENGTH ? s : s.substring(0, MAX_LENGTH / 2) + "..." + s.substring(s.length() - MAX_LENGTH / 2 - 3);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ProjectItem)) return false;
      ProjectItem item = (ProjectItem)o;
      if (!myProjectSettings.equals(item.myProjectSettings)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return myProjectSettings.hashCode();
    }
  }

  private void updateTree(@Nullable CachingSimpleNode nodeToUpdate) {
    Set<CachingSimpleNode> toUpdate = ContainerUtil.newIdentityTroveSet();
    if (nodeToUpdate == null) {
      for (DefaultMutableTreeNode node : myTree.getSelectedNodes(DefaultMutableTreeNode.class, null)) {
        final Object userObject = node.getUserObject();
        if (userObject instanceof SimpleNode && ((SimpleNode)userObject).getParent() instanceof CachingSimpleNode) {
          toUpdate.add((CachingSimpleNode)((SimpleNode)userObject).getParent());
        }
      }
    }
    else {
      toUpdate.add(nodeToUpdate);
    }

    if (toUpdate.isEmpty()) {
      toUpdate.add(myRootNode);
    }

    Element treeStateElement = new Element("root");
    try {
      TreeState.createOn(myTree).writeExternal(treeStateElement);
    }
    catch (WriteExternalException ignore) {
    }

    for (CachingSimpleNode node : toUpdate) {
      cleanUpEmptyNodes(node);
    }

    TreeState.createFrom(treeStateElement).applyTo(myTree);
  }

  private void cleanUpEmptyNodes(@NotNull CachingSimpleNode node) {
    node.cleanUpCache();
    treeBuilder.addSubtreeToUpdateByElement(node);
    if (node.getChildren().length == 0) {
      if (node.getParent() instanceof CachingSimpleNode) {
        cleanUpEmptyNodes((CachingSimpleNode)node.getParent());
      }
    }
  }

  private static class ProjectPopupItem {
    ModuleData myModuleData;
    List<String> myTasks;

    ProjectPopupItem(ModuleData moduleData, List<String> tasks) {
      myModuleData = moduleData;
      myTasks = tasks;
    }

    @Override
    public String toString() {
      return myModuleData.getId();
    }
  }

  private class ChooseProjectStep extends BaseListPopupStep<ProjectPopupItem> {
    protected ChooseProjectStep(List<? extends ProjectPopupItem> values) {
      super("Choose project", values);
    }

    @Override
    public PopupStep onChosen(final ProjectPopupItem projectPopupItem, final boolean finalChoice) {
      return new BaseListPopupStep<Phase>("Choose activation phase", Phase.values()) {
        @Override
        public PopupStep onChosen(final Phase selectedPhase, boolean finalChoice) {
          final Map<String, TaskActivationState> activationMap =
            getInstance(myProject).getStateProvider().getProjectsTasksActivationMap(myProjectSystemId);
          final String projectPath = projectPopupItem.myModuleData.getLinkedExternalProjectPath();
          final List<String> tasks = activationMap.get(projectPath).getTasks(selectedPhase);

          final List<String> tasksToSuggest = ContainerUtil.newArrayList(projectPopupItem.myTasks);
          tasksToSuggest.removeAll(tasks);
          return new BaseListPopupStep<String>("Choose task", tasksToSuggest) {
            @Override
            public PopupStep onChosen(final String taskName, boolean finalChoice) {
              return doFinalStep(() -> {
                myTaskActivator.addTask(new TaskActivationEntry(myProjectSystemId, selectedPhase, projectPath, taskName));
                updateTree(myRootNode);
              });
            }
          };
        }

        @Override
        public boolean hasSubstep(Phase phase) {
          return true;
        }
      };
    }

    @Override
    public boolean hasSubstep(ProjectPopupItem selectedValue) {
      return true;
    }
  }

  private abstract static class MyNode extends CachingSimpleNode {
    protected MyNode(SimpleNode aParent) {
      super(aParent);
    }

    MyNode(Project aProject, @Nullable NodeDescriptor aParentDescriptor) {
      super(aProject, aParentDescriptor);
    }
  }

  private class RootNode extends MyNode {
    private final ExternalProjectsStateProvider myStateProvider;

    RootNode() {
      super(ConfigureTasksActivationDialog.this.myProject, null);
      myStateProvider = getInstance(ConfigureTasksActivationDialog.this.myProject).getStateProvider();
    }

    @Override
    public boolean isAutoExpandNode() {
      return true;
    }

    @Override
    protected MyNode[] buildChildren() {
      ProjectItem item = (ProjectItem)projectCombobox.getSelectedItem();
      if(item == null) return new MyNode[]{};
      if (item.myProjectSettings.getModules().isEmpty() || item.myProjectSettings.getModules().size() == 1) {
        final TaskActivationState tasksActivation =
          myStateProvider.getTasksActivation(myProjectSystemId, item.myProjectSettings.getExternalProjectPath());
        return buildProjectPhasesNodes(item.myProjectSettings.getExternalProjectPath(), tasksActivation, this);
      }
      else {
        return buildProjectsNodes(item.myProjectSettings, myStateProvider, this);
      }
    }
  }

  private class ProjectNode extends MyNode {
    private final ExternalProjectsStateProvider myStateProvider;
    private final String myRootProjectPath;
    private final String myProjectPath;
    private final String myProjectName;

    ProjectNode(RootNode parent,
                       ExternalProjectsStateProvider stateProvider,
                       String rootProjectPath,
                       String projectPath) {
      super(parent);
      myStateProvider = stateProvider;
      myProjectPath = projectPath;
      myRootProjectPath = rootProjectPath;
      myProjectName = uiAware.getProjectRepresentationName(myProjectPath, myRootProjectPath);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(ExternalSystemIcons.TaskGroup);
    }

    @Override
    public String getName() {
      return myProjectName;
    }

    @Override
    protected MyNode[] buildChildren() {
      final TaskActivationState tasksActivation = myStateProvider.getTasksActivation(myProjectSystemId, myProjectPath);
      return buildProjectPhasesNodes(myProjectPath, tasksActivation, this);
    }
  }

  private class PhaseNode extends MyNode {
    private final Phase myPhase;
    private final TaskActivationState myTaskActivationState;
    private final String myProjectPath;

    PhaseNode(final String projectPath, Phase phase, TaskActivationState taskActivationState, SimpleNode parent) {
      super(parent);
      myPhase = phase;
      myTaskActivationState = taskActivationState;
      myProjectPath = projectPath;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(ExternalSystemIcons.TaskGroup);
    }

    @Override
    public boolean isAutoExpandNode() {
      return true;
    }

    @Override
    public MyNode[] buildChildren() {
      return ContainerUtil.map2Array(myTaskActivationState.getTasks(myPhase), MyNode.class,
                                     (Function<String, MyNode>)taskName -> new TaskNode(taskName, this));
    }

    @Override
    public String getName() {
      return myPhase.toString();
    }
  }

  private class TaskNode extends MyNode {
    private final String myTaskName;

    TaskNode(String taskName, PhaseNode parent) {
      super(parent);
      myTaskName = taskName;
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
      super.update(presentation);
      presentation.setIcon(uiAware.getTaskIcon());
    }

    @Override
    public MyNode[] buildChildren() {
      return new MyNode[0];
    }

    @Override
    public String getName() {
      return myTaskName;
    }

    @Override
    public boolean isAlwaysLeaf() {
      return true;
    }
  }
}