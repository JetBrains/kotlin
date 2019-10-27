// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view;

import com.intellij.execution.Location;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil;
import com.intellij.openapi.externalSystem.action.ExternalSystemViewGearAction;
import com.intellij.openapi.externalSystem.model.*;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemTaskLocation;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.pom.Navigatable;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.mac.TouchbarDataKeys;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBusConnection;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectsViewImpl extends SimpleToolWindowPanel implements DataProvider, ExternalProjectsView, Disposable {
  public static final Logger LOG = Logger.getInstance(ExternalProjectsViewImpl.class);

  @NotNull
  private final Project myProject;
  @NotNull
  private final ExternalProjectsManagerImpl myProjectsManager;
  @NotNull
  private final ToolWindowEx myToolWindow;
  @NotNull
  private final ProjectSystemId myExternalSystemId;
  @NotNull
  private final ExternalSystemUiAware myUiAware;
  @NotNull
  private final Set<Listener> listeners = new HashSet<>();

  @Nullable
  private ExternalProjectsStructure myStructure;
  private SimpleTree myTree;
  @NotNull
  private final NotificationGroup myNotificationGroup;
  private final List<ExternalSystemViewContributor> myViewContributors;

  private ExternalProjectsViewState myState = new ExternalProjectsViewState();

  public ExternalProjectsViewImpl(@NotNull Project project, @NotNull ToolWindowEx toolWindow, @NotNull ProjectSystemId externalSystemId) {
    super(true, true);
    myProject = project;
    myToolWindow = toolWindow;
    myExternalSystemId = externalSystemId;
    myUiAware = ExternalSystemUiUtil.getUiAware(externalSystemId);
    myProjectsManager = ExternalProjectsManagerImpl.getInstance(myProject);

    String toolWindowId =
      toolWindow instanceof ToolWindowImpl ? ((ToolWindowImpl)toolWindow).getId() : myExternalSystemId.getReadableName();

    String notificationId = "notification.group.id." + StringUtil.toLowerCase(externalSystemId.getId());
    NotificationGroup registeredGroup = NotificationGroup.findRegisteredGroup(notificationId);
    myNotificationGroup = registeredGroup != null ? registeredGroup : NotificationGroup.toolWindowGroup(notificationId, toolWindowId);
    myViewContributors = Arrays.stream(ExternalSystemViewContributor.EP_NAME.getExtensions())
                               .filter(c -> ProjectSystemId.IDE.equals(c.getSystemId()) ||
                                            myExternalSystemId.equals(c.getSystemId()))
                               .collect(Collectors.toList());

    setName(myExternalSystemId.getReadableName() + " tool window");
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    if (ExternalSystemDataKeys.VIEW.is(dataId)) return this;

    if (PlatformDataKeys.HELP_ID.is(dataId)) return "reference.toolwindows.gradle";
    if (CommonDataKeys.PROJECT.is(dataId)) return myProject;
    if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) return extractVirtualFile();
    if (CommonDataKeys.VIRTUAL_FILE_ARRAY.is(dataId)) return extractVirtualFiles();
    if (Location.DATA_KEY.is(dataId)) {
      return extractLocation();
    }
    if (CommonDataKeys.NAVIGATABLE_ARRAY.is(dataId)) return extractNavigatables();

    if (ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID.is(dataId)) return myExternalSystemId;
    if (ExternalSystemDataKeys.UI_AWARE.is(dataId)) return myUiAware;
    if (ExternalSystemDataKeys.SELECTED_PROJECT_NODE.is(dataId)) return getSelectedProjectNode();
    if (ExternalSystemDataKeys.SELECTED_NODES.is(dataId)) return getSelectedNodes(ExternalSystemNode.class);
    if (ExternalSystemDataKeys.PROJECTS_TREE.is(dataId)) return myTree;
    if (ExternalSystemDataKeys.NOTIFICATION_GROUP.is(dataId)) return myNotificationGroup;
    if (TouchbarDataKeys.ACTIONS_KEY.is(dataId)) return new DefaultActionGroup(ActionManager.getInstance().getAction("ExternalSystem.RefreshAllProjects"));

    return super.getData(dataId);
  }

  @Override
  @NotNull
  public Project getProject() {
    return myProject;
  }

  @Override
  @NotNull
  public ExternalSystemUiAware getUiAware() {
    return myUiAware;
  }

  @Override
  public ExternalSystemShortcutsManager getShortcutsManager() {
    return myProjectsManager.getShortcutsManager();
  }

  @Override
  public ExternalSystemTaskActivator getTaskActivator() {
    return myProjectsManager.getTaskActivator();
  }

  @Override
  @NotNull
  public ProjectSystemId getSystemId() {
    return myExternalSystemId;
  }

  @NotNull
  public NotificationGroup getNotificationGroup() {
    return myNotificationGroup;
  }

  public void init() {
    Disposer.register(myProject, this);
    initTree();

    MessageBusConnection busConnection = myProject.getMessageBus().connect(this);
    busConnection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
      boolean wasVisible = false;

      @Override
      public void stateChanged() {
        if (myToolWindow.isDisposed()) return;
        boolean visible = myToolWindow.isVisible();
        if (!visible || wasVisible) {
          wasVisible = visible;
          return;
        }
        scheduleStructureUpdate();
        wasVisible = true;
      }
    });

    getShortcutsManager().addListener(() -> {
      scheduleTasksUpdate();

      scheduleStructureRequest(() -> {
        assert myStructure != null;
        myStructure.updateNodes(RunConfigurationNode.class);
      });
    });

    getTaskActivator().addListener(new ExternalSystemTaskActivator.Listener() {
      @Override
      public void tasksActivationChanged() {
        scheduleTasksUpdate();

        scheduleStructureRequest(() -> {
          assert myStructure != null;
          myStructure.updateNodes(RunConfigurationNode.class);
        });
      }
    });

    busConnection.subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
      private void changed() {
        scheduleStructureRequest(() -> {
          assert myStructure != null;
          myStructure.visitNodes(ModuleNode.class, node -> node.updateRunConfigurations());
        });
      }

      @Override
      public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
        changed();
      }

      @Override
      public void runConfigurationRemoved(@NotNull RunnerAndConfigurationSettings settings) {
        changed();
      }

      @Override
      public void runConfigurationChanged(@NotNull RunnerAndConfigurationSettings settings) {
        changed();
      }
    });

    ExternalSystemApiUtil.subscribe(myProject, myExternalSystemId, new ExternalSystemSettingsListenerAdapter() {
      @Override
      public void onUseAutoImportChange(boolean currentValue, @NotNull final String linkedProjectPath) {
        scheduleStructureRequest(() -> {
          assert myStructure != null;
          final List<ProjectNode> projectNodes = myStructure.getNodes(ProjectNode.class);
          for (ProjectNode projectNode : projectNodes) {
            final ProjectData projectData = projectNode.getData();
            if (projectData != null && projectData.getLinkedExternalProjectPath().equals(linkedProjectPath)) {
              projectNode.updateProject();
              break;
            }
          }
        });
      }
    });

    myToolWindow.setAdditionalGearActions(createAdditionalGearActionsGroup());

    scheduleStructureUpdate();
  }

  @Override
  public void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
    if (actionId != null) {
      ExternalSystemActionUtil.executeAction(actionId, getName(), inputEvent);
    }
    for (Listener listener : listeners) {
      listener.onDoubleClickOrEnter(node, inputEvent);
    }
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    listeners.add(listener);
  }

  private ActionGroup createAdditionalGearActionsGroup() {
    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup group = new DefaultActionGroup();
    String[] ids = new String[]{"ExternalSystem.GroupModules", "ExternalSystem.GroupTasks", "ExternalSystem.ShowInheritedTasks", "ExternalSystem.ShowIgnored"};
    for (String id : ids) {
      final AnAction gearAction = actionManager.getAction(id);
      if (gearAction instanceof ExternalSystemViewGearAction) {
        ((ExternalSystemViewGearAction)gearAction).setView(this);
        group.add(gearAction);
        Disposer.register(myProject, new Disposable() {
          @Override
          public void dispose() {
            ((ExternalSystemViewGearAction)gearAction).setView(null);
          }
        });
      }
    }
    return group;
  }

  @ApiStatus.Internal
  public void initStructure() {
    myStructure = new ExternalProjectsStructure(myProject, myTree);
    Disposer.register(this, myStructure);
    myStructure.init(this);
  }

  private void initTree() {
    myTree = new ExternalProjectTree(myProject);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

    final ActionManager actionManager = ActionManager.getInstance();
    ActionToolbar actionToolbar = actionManager.createActionToolbar(myExternalSystemId.getReadableName() + " View Toolbar",
                                                                    (DefaultActionGroup)actionManager
                                                                      .getAction("ExternalSystemView.ActionsToolbar"), true);
    // make the view data context available for the toolbar actions
    actionToolbar.setTargetComponent(this);
    setToolbar(actionToolbar.getComponent());
    setContent(ScrollPaneFactory.createScrollPane(myTree));

    myTree.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(final Component comp, final int x, final int y) {
        final String id = getMenuId(getSelectedNodes(ExternalSystemNode.class));
        if (id != null) {
          final ActionGroup actionGroup = (ActionGroup)actionManager.getAction(id);
          if (actionGroup != null) {
            actionManager.createActionPopupMenu(ExternalProjectsViewImpl.this.getName(), actionGroup).getComponent().show(comp, x, y);
          }
        }
      }

      @Nullable
      private String getMenuId(Collection<? extends ExternalSystemNode> nodes) {
        String id = null;
        for (ExternalSystemNode node : nodes) {
          String menuId = node.getMenuId();
          if (menuId == null) {
            return null;
          }
          if (id == null) {
            id = menuId;
          }
          else if (!id.equals(menuId)) {
            return null;
          }
        }
        return id;
      }
    });
  }

  public void scheduleStructureUpdate() {
    scheduleStructureRequest(() -> {
      final Collection<ExternalProjectInfo> projectsData =
        ProjectDataManager.getInstance().getExternalProjectsData(myProject, myExternalSystemId);

      final List<DataNode<ProjectData>> toImport =
        ContainerUtil.mapNotNull(projectsData, info -> info.getExternalProjectStructure());

      assert myStructure != null;
      myStructure.updateProjects(toImport);
    });
  }

  protected boolean isUnitTestMode() {
    return ApplicationManager.getApplication().isUnitTestMode();
  }

  public static void invokeLater(Project p, Runnable r) {
    invokeLater(p, ModalityState.defaultModalityState(), r);
  }

  public static void invokeLater(final Project p, final ModalityState state, final Runnable r) {
    if (isNoBackgroundMode()) {
      r.run();
    }
    else {
      ApplicationManager.getApplication().invokeLater(r, state, p.getDisposed());
    }
  }

  public static boolean isNoBackgroundMode() {
    return (ApplicationManager.getApplication().isUnitTestMode()
            || ApplicationManager.getApplication().isHeadlessEnvironment());
  }

  @Override
  public void updateUpTo(ExternalSystemNode node) {
    ExternalProjectsStructure structure = getStructure();
    if (structure != null) {
      structure.updateUpTo(node);
    }
  }

  @Override
  @Nullable
  public ExternalProjectsStructure getStructure() {
    return myStructure;
  }

  @Override
  @NotNull
  public List<ExternalSystemNode<?>> createNodes(@NotNull ExternalProjectsView externalProjectsView,
                                                 @Nullable ExternalSystemNode<?> parent,
                                                 @NotNull DataNode<?> dataNode) {
    final List<ExternalSystemNode<?>> result = new SmartList<>();
    final MultiMap<Key<?>, DataNode<?>> groups = ExternalSystemApiUtil.group(dataNode.getChildren());
    for (ExternalSystemViewContributor contributor : myViewContributors) {
      final MultiMap<Key<?>, DataNode<?>> dataNodes = new ContainerUtil.KeyOrderedMultiMap<>();
      for (Key<?> key : contributor.getKeys()) {
        ContainerUtil.putIfNotNull(key, groups.get(key), dataNodes);
      }

      if (dataNodes.isEmpty()) continue;

      final List<ExternalSystemNode<?>> childNodes = contributor.createNodes(externalProjectsView, dataNodes);
      result.addAll(childNodes);

      if (parent == null) continue;

      for (ExternalSystemNode childNode : childNodes) {
        childNode.setParent(parent);
      }
    }

    return result;
  }

  @Override
  public ExternalProjectsStructure.ErrorLevel getErrorLevelRecursively(@NotNull DataNode node) {
    Ref<ExternalProjectsStructure.ErrorLevel> ref = new Ref<>(ExternalProjectsStructure.ErrorLevel.NONE);
    ExternalSystemApiUtil.visit(node, currentNode -> {
      for (ExternalSystemViewContributor contributor : myViewContributors) {
        ExternalProjectsStructure.ErrorLevel errorLevel = contributor.getErrorLevel(currentNode);
        if (ref.get() == null || errorLevel.compareTo(ref.get()) > 0) {
          ref.set(errorLevel);
        }
      }
    });
    return ref.get();
  }

  @Nullable
  public ExternalProjectsViewState getState() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myStructure != null) {
      try {
        myState.treeState = new Element("root");
        TreeState.createOn(myTree).writeExternal(myState.treeState);
      }
      catch (WriteExternalException e) {
        LOG.warn(e);
      }
    }
    return myState;
  }

  public void loadState(ExternalProjectsViewState state) {
    myState = state;
  }

  @Override
  public boolean getShowIgnored() {
    return myState.showIgnored;
  }

  public void setShowIgnored(boolean value) {
    if (myState.showIgnored != value) {
      myState.showIgnored = value;
      scheduleStructureUpdate();
    }
  }

  @Override
  public boolean getGroupTasks() {
    return myState.groupTasks;
  }

  @Override
  public boolean getGroupModules() {
    return myState.groupModules;
  }

  @Override
  public boolean useTasksNode() {
    return true;
  }

  public void setGroupTasks(boolean value) {
    if (myState.groupTasks != value) {
      myState.groupTasks = value;
      scheduleNodesRebuild(TasksNode.class);
    }
  }

  public void setGroupModules(boolean value) {
    if (myState.groupModules != value) {
      myState.groupModules = value;
      scheduleNodesRebuild(ModuleNode.class);
      scheduleNodesRebuild(ProjectNode.class);
    }
  }

  @Override
  public boolean showInheritedTasks() {
    return myState.showInheritedTasks;
  }

  public void setShowInheritedTasks(boolean value) {
    if (myState.showInheritedTasks != value) {
      myState.showInheritedTasks = value;
      scheduleStructureUpdate();
    }
  }

  @Override
  @Nullable
  public String getDisplayName(@Nullable DataNode node) {
    if (node == null) return null;
    return myViewContributors.stream()
                             .map(contributor -> contributor.getDisplayName(node))
                             .filter(Objects::nonNull)
                             .findFirst()
                             .orElse(null);
  }

  private <T extends ExternalSystemNode> void scheduleNodesRebuild(@NotNull Class<T> nodeClass) {
    scheduleStructureRequest(() -> {
      assert myStructure != null;
      for (T tasksNode : myStructure.getNodes(nodeClass)) {
        tasksNode.cleanUpCache();
      }
      myStructure.updateNodes(nodeClass);
    });
  }

  private void scheduleTasksUpdate() {
    scheduleStructureRequest(() -> {
      assert myStructure != null;
      myStructure.updateNodes(TaskNode.class);
    });
  }

  private void scheduleStructureRequest(final Runnable r) {
    invokeLater(myProject, () -> {
      if (!myToolWindow.isVisible()) return;

      boolean shouldCreate = myStructure == null;
      if (shouldCreate) {
        initStructure();
      }

      myTree.setPaintBusy(true);
      try {
        r.run();
        if (shouldCreate) {
          restoreTreeState();
        }
      }
      finally {
        myTree.setPaintBusy(false);
      }
    });
  }

  private void restoreTreeState() {
    TreeState.createFrom(myState.treeState).applyTo(myTree);
  }

  private <T extends ExternalSystemNode> List<T> getSelectedNodes(Class<T> aClass) {
    return myStructure != null ? myStructure.getSelectedNodes(myTree, aClass) : ContainerUtil.emptyList();
  }

  private List<ProjectNode> getSelectedProjectNodes() {
    return getSelectedNodes(ProjectNode.class);
  }

  @Nullable
  private ProjectNode getSelectedProjectNode() {
    final List<ProjectNode> projectNodes = getSelectedProjectNodes();
    return projectNodes.size() == 1 ? projectNodes.get(0) : null;
  }

  @Nullable
  private ExternalSystemTaskLocation extractLocation() {
    final List<ExternalSystemNode> selectedNodes = getSelectedNodes(ExternalSystemNode.class);
    if (selectedNodes.isEmpty()) return null;

    List<TaskData> tasks = new SmartList<>();

    ExternalTaskExecutionInfo taskExecutionInfo = new ExternalTaskExecutionInfo();

    String projectPath = null;

    for (ExternalSystemNode node : selectedNodes) {
      final Object data = node.getData();
      if (data instanceof TaskData) {
        final TaskData taskData = (TaskData)data;
        if (projectPath == null) {
          projectPath = taskData.getLinkedExternalProjectPath();
        }
        else if (!taskData.getLinkedExternalProjectPath().equals(projectPath)) {
          return null;
        }

        taskExecutionInfo.getSettings().getTaskNames().add(taskData.getName());
        taskExecutionInfo.getSettings().getTaskDescriptions().add(taskData.getDescription());
        tasks.add(taskData);
      }
    }

    if (tasks.isEmpty()) return null;

    taskExecutionInfo.getSettings().setExternalSystemIdString(myExternalSystemId.toString());
    taskExecutionInfo.getSettings().setExternalProjectPath(projectPath);

    return ExternalSystemTaskLocation.create(myProject, myExternalSystemId, projectPath, taskExecutionInfo);
  }

  private VirtualFile extractVirtualFile() {
    for (ExternalSystemNode each : getSelectedNodes(ExternalSystemNode.class)) {
      VirtualFile file = each.getVirtualFile();
      if (file != null && file.isValid()) return file;
    }

    final ProjectNode projectNode = getSelectedProjectNode();
    if (projectNode == null) return null;
    VirtualFile file = projectNode.getVirtualFile();
    if (file == null || !file.isValid()) return null;
    return file;
  }

  private Object extractVirtualFiles() {
    final List<VirtualFile> files = new ArrayList<>();
    for (ExternalSystemNode each : getSelectedNodes(ExternalSystemNode.class)) {
      VirtualFile file = each.getVirtualFile();
      if (file != null && file.isValid()) files.add(file);
    }
    return files.isEmpty() ? null : VfsUtilCore.toVirtualFileArray(files);
  }

  private Object extractNavigatables() {
    final List<Navigatable> navigatables = new ArrayList<>();
    for (ExternalSystemNode each : getSelectedNodes(ExternalSystemNode.class)) {
      Navigatable navigatable = each.getNavigatable();
      if (navigatable != null) navigatables.add(navigatable);
    }
    return navigatables.isEmpty() ? null : navigatables.toArray(new Navigatable[0]);
  }

  @Override
  public void dispose() {
    this.listeners.clear();
    this.myStructure = null;
    this.myTree = null;
  }
}
