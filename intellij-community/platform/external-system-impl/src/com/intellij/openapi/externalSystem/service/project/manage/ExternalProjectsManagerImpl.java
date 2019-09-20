// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.project.autoimport.ExternalSystemProjectsWatcher;
import com.intellij.openapi.externalSystem.service.project.autoimport.ExternalSystemProjectsWatcherImpl;
import com.intellij.openapi.externalSystem.util.CompositeRunnable;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.view.ExternalProjectsView;
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl;
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.ExternalStorageConfigurationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.ModuleRootManagerImpl;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.intellij.openapi.externalSystem.model.ProjectKeys.MODULE;
import static com.intellij.openapi.externalSystem.model.ProjectKeys.TASK;

/**
 * @author Vladislav.Soroka
 */
@State(name = "ExternalProjectsManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class ExternalProjectsManagerImpl implements ExternalProjectsManager, PersistentStateComponent<ExternalProjectsState>, Disposable {
  private static final Logger LOG = Logger.getInstance(ExternalProjectsManager.class);

  private final AtomicBoolean isInitializationFinished = new AtomicBoolean();
  private final AtomicBoolean isInitializationStarted = new AtomicBoolean();
  private final AtomicBoolean isDisposed = new AtomicBoolean();
  private final CompositeRunnable myPostInitializationActivities = new CompositeRunnable();
  @NotNull
  private ExternalProjectsState myState = new ExternalProjectsState();

  @NotNull
  private final Project myProject;
  private final ExternalSystemRunManagerListener myRunManagerListener;
  private final ExternalSystemTaskActivator myTaskActivator;
  private final ExternalSystemShortcutsManager myShortcutsManager;
  private final List<ExternalProjectsView> myProjectsViews = new SmartList<>();
  private final ExternalSystemProjectsWatcherImpl myWatcher;

  public ExternalProjectsManagerImpl(@NotNull Project project) {
    myProject = project;
    myShortcutsManager = new ExternalSystemShortcutsManager(project);
    Disposer.register(this, myShortcutsManager);
    myTaskActivator = new ExternalSystemTaskActivator(project);
    myRunManagerListener = new ExternalSystemRunManagerListener(this);
    myWatcher = new ExternalSystemProjectsWatcherImpl(myProject);
    Disposer.register(this, myWatcher);
  }

  public static ExternalProjectsManagerImpl getInstance(@NotNull Project project) {
    return (ExternalProjectsManagerImpl)project.getService(ExternalProjectsManager.class);
  }

  @Nullable
  public static Project setupCreatedProject(@Nullable Project project) {
    if (project != null) {
      getInstance(project).setStoreExternally(true);
    }
    return project;
  }

  public void setStoreExternally(boolean value) {
    ExternalStorageConfigurationManager externalStorageConfigurationManager =
      ExternalStorageConfigurationManager.getInstance(myProject);
    if (externalStorageConfigurationManager.isEnabled() == value) return;
    externalStorageConfigurationManager.setEnabled(value);

    // force re-save
    try {
      for (Module module : ModuleManager.getInstance(myProject).getModules()) {
        if (!module.isDisposed()) {
          ExternalSystemModulePropertyManager.getInstance(module).swapStore();
          ((ModuleRootManagerImpl)ModuleRootManager.getInstance(module)).stateChanged();
        }
      }
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  @NotNull
  @Override
  public Project getProject() {
    return myProject;
  }

  public ExternalSystemShortcutsManager getShortcutsManager() {
    return myShortcutsManager;
  }

  public ExternalSystemTaskActivator getTaskActivator() {
    return myTaskActivator;
  }

  @Override
  public ExternalSystemProjectsWatcher getExternalProjectsWatcher() {
    return myWatcher;
  }

  public void registerView(@NotNull ExternalProjectsView externalProjectsView) {
    assert getExternalProjectsView(externalProjectsView.getSystemId()) == null;

    myProjectsViews.add(externalProjectsView);
    if (externalProjectsView instanceof ExternalProjectsViewImpl) {
      ExternalProjectsViewImpl view = (ExternalProjectsViewImpl)externalProjectsView;
      view.loadState(myState.getExternalSystemsState().get(externalProjectsView.getSystemId().getId()).getProjectsViewState());
      view.init();
    }
  }

  @Nullable
  public ExternalProjectsView getExternalProjectsView(@NotNull ProjectSystemId systemId) {
    for (ExternalProjectsView projectsView : myProjectsViews) {
      if (projectsView.getSystemId().equals(systemId)) return projectsView;
    }
    return null;
  }

  public void init() {
    ProgressManager.checkCanceled();

    if (isInitializationStarted.getAndSet(true)) {
      return;
    }
    myWatcher.start();

    // load external projects data
    ExternalProjectsDataStorage.getInstance(myProject).load();
    myRunManagerListener.attach();

    // init shortcuts manager
    myShortcutsManager.init();
    for (ExternalSystemManager<?, ?, ?, ?, ?> systemManager : ExternalSystemManager.EP_NAME.getIterable()) {
      Collection<ExternalProjectInfo> externalProjects = ExternalProjectsDataStorage.getInstance(myProject).list(systemManager.getSystemId());
      for (ExternalProjectInfo externalProject : externalProjects) {
        if (externalProject.getExternalProjectStructure() == null) {
          continue;
        }
        Collection<DataNode<TaskData>> taskData = ExternalSystemApiUtil.findAllRecursively(externalProject.getExternalProjectStructure(), TASK);
        myShortcutsManager.scheduleKeymapUpdate(taskData);
      }

      if (!externalProjects.isEmpty()) {
        myShortcutsManager.scheduleRunConfigurationKeymapUpdate(systemManager.getSystemId());
      }
    }
    // init task activation info
    myTaskActivator.init();

    synchronized (isInitializationFinished) {
      isInitializationFinished.set(true);
      invokeLater(() -> {
        myPostInitializationActivities.run();
        myPostInitializationActivities.clear();
      });
    }
  }

  @Override
  public void refreshProject(@NotNull final String externalProjectPath, @NotNull final ImportSpec importSpec) {
    ExternalSystemUtil.refreshProject(externalProjectPath, importSpec);
  }

  @Override
  public void runWhenInitialized(@NotNull Runnable runnable) {
    if (isDisposed.get()) return;
    synchronized (isInitializationFinished) {
      if (isInitializationFinished.get()) {
        invokeLater(runnable);
      }
      else {
        myPostInitializationActivities.add(runnable);
      }
    }
  }

  private void invokeLater(@NotNull Runnable runnable) {
    ApplicationManager.getApplication().invokeLater(runnable, o -> myProject.isDisposed() || isDisposed.get());
  }

  public void updateExternalProjectData(ExternalProjectInfo externalProject) {
    // update external projects data
    ExternalProjectsDataStorage.getInstance(myProject).update(externalProject);

    // update shortcuts manager
    if (externalProject.getExternalProjectStructure() != null) {
      final ProjectData projectData = externalProject.getExternalProjectStructure().getData();

      ExternalSystemUtil.scheduleExternalViewStructureUpdate(myProject, projectData.getOwner());

      Collection<DataNode<TaskData>> taskData =
        ExternalSystemApiUtil.findAllRecursively(externalProject.getExternalProjectStructure(), TASK);
      myShortcutsManager.scheduleKeymapUpdate(taskData);
      myShortcutsManager.scheduleRunConfigurationKeymapUpdate(projectData.getOwner());
    }
  }

  public void forgetExternalProjectData(@NotNull ProjectSystemId projectSystemId, @NotNull String linkedProjectPath) {
    ExternalProjectsDataStorage.getInstance(myProject).remove(projectSystemId, linkedProjectPath);
    ExternalSystemUtil.scheduleExternalViewStructureUpdate(myProject, projectSystemId);
  }

  @NotNull
  @Override
  public ExternalProjectsState getState() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    for (ExternalProjectsView externalProjectsView : myProjectsViews) {
      if (externalProjectsView instanceof ExternalProjectsViewImpl) {
        final ExternalProjectsViewState externalProjectsViewState = ((ExternalProjectsViewImpl)externalProjectsView).getState();
        final ExternalProjectsState.State state = myState.getExternalSystemsState().get(externalProjectsView.getSystemId().getId());
        assert state != null;
        state.setProjectsViewState(externalProjectsViewState);
      }
    }
    return myState;
  }

  @NotNull
  public ExternalProjectsStateProvider getStateProvider() {
    return new ExternalProjectsStateProvider() {
      @Override
      public List<TasksActivation> getAllTasksActivation() {
        List<TasksActivation> result = new SmartList<>();
        Map<String, ProjectSystemId> systemIds = ExternalSystemApiUtil.getAllManagers().stream()
          .collect(Collectors.toMap(o -> o.getSystemId().getId(), o -> o.getSystemId()));
        for (Map.Entry<String, ExternalProjectsState.State> systemState : myState.getExternalSystemsState().entrySet()) {
          ProjectSystemId systemId = systemIds.get(systemState.getKey());
          if (systemId == null) continue;

          for (Map.Entry<String, TaskActivationState> activationStateEntry : systemState.getValue().getExternalSystemsTaskActivation()
            .entrySet()) {
            result.add(new TasksActivation(systemId, activationStateEntry.getKey(), activationStateEntry.getValue()));
          }
        }

        return result;
      }

      @Override
      public List<TasksActivation> getTasksActivation(@NotNull final ProjectSystemId systemId) {
        final Set<Map.Entry<String, TaskActivationState>> entries =
          myState.getExternalSystemsState().get(systemId.getId()).getExternalSystemsTaskActivation().entrySet();
        return ContainerUtil.map(entries, entry -> new TasksActivation(systemId, entry.getKey(), entry.getValue()));
      }

      @Override
      public TaskActivationState getTasksActivation(@NotNull ProjectSystemId systemId, @NotNull String projectPath) {
        return myState.getExternalSystemsState().get(systemId.getId()).getExternalSystemsTaskActivation().get(projectPath);
      }

      @Override
      public Map<String, TaskActivationState> getProjectsTasksActivationMap(@NotNull final ProjectSystemId systemId) {
        return myState.getExternalSystemsState().get(systemId.getId()).getExternalSystemsTaskActivation();
      }
    };
  }

  @Override
  public boolean isIgnored(@NotNull ProjectSystemId systemId, @NotNull String projectPath) {
    final ExternalProjectInfo projectInfo = ExternalSystemUtil.getExternalProjectInfo(myProject, systemId, projectPath);
    if (projectInfo == null) return true;

    return ExternalProjectsDataStorage.getInstance(myProject).isIgnored(projectInfo.getExternalProjectPath(), projectPath, MODULE);
  }

  @Override
  public void setIgnored(@NotNull DataNode<?> dataNode, boolean isIgnored) {
    ExternalProjectsDataStorage.getInstance(myProject).setIgnored(dataNode, isIgnored);
    ExternalSystemKeymapExtension.updateActions(myProject, ExternalSystemApiUtil.findAllRecursively(dataNode, TASK));
  }

  @Override
  public void loadState(@NotNull ExternalProjectsState state) {
    myState = state;
    // migrate to new
    if (myState.storeExternally) {
      myState.storeExternally = false;
      ExternalStorageConfigurationManager.getInstance(myProject).setEnabled(true);
    }
  }

  @Override
  public void dispose() {
    if (isDisposed.getAndSet(true)) return;
    myPostInitializationActivities.clear();
    myProjectsViews.clear();
    myRunManagerListener.detach();
  }

  public interface ExternalProjectsStateProvider {
    class TasksActivation {
      public final ProjectSystemId systemId;
      public final String projectPath;
      public final TaskActivationState state;

      public TasksActivation(ProjectSystemId systemId,
                             String projectPath,
                             TaskActivationState state) {
        this.systemId = systemId;
        this.projectPath = projectPath;
        this.state = state;
      }
    }

    List<TasksActivation> getAllTasksActivation();

    List<TasksActivation> getTasksActivation(@NotNull ProjectSystemId systemId);

    TaskActivationState getTasksActivation(@NotNull ProjectSystemId systemId, @NotNull String projectPath);

    Map<String, TaskActivationState> getProjectsTasksActivationMap(@NotNull ProjectSystemId systemId);
  }

  public static void disableProjectWatcherAutoUpdate(@NotNull Project project) {
    ExternalProjectsManagerImpl projectsManager = getInstance(project);
    projectsManager.myWatcher.disableAutoUpdate();
  }
}
