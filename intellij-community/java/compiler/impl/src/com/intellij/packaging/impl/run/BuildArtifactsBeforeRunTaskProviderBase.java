// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packaging.impl.run;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.packaging.artifacts.*;
import com.intellij.task.ProjectTask;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.impl.ProjectTaskManagerImpl;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class BuildArtifactsBeforeRunTaskProviderBase<T extends BuildArtifactsBeforeRunTaskBase>
  extends BeforeRunTaskProvider<T> {
  private final Project myProject;
  @NotNull final private Class<T> myTaskClass;

  public BuildArtifactsBeforeRunTaskProviderBase(@NotNull Class<T> taskClass, Project project) {
    myProject = project;
    myTaskClass = taskClass;
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactAdapter() {
      @Override
      public void artifactRemoved(@NotNull Artifact artifact) {
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(myProject);
        for (RunConfiguration configuration : runManager.getAllConfigurationsList()) {
          final List<T> tasks = runManager.getBeforeRunTasks(configuration, getId());
          for (T task : tasks) {
            final String artifactName = artifact.getName();
            final List<ArtifactPointer> pointersList = task.getArtifactPointers();
            final ArtifactPointer[] pointers = pointersList.toArray(new ArtifactPointer[0]);
            for (ArtifactPointer pointer : pointers) {
              if (pointer.getArtifactName().equals(artifactName) &&
                  ArtifactManager.getInstance(myProject).findArtifact(artifactName) == null) {
                task.removeArtifact(pointer);
              }
            }
          }
        }
      }
    });
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public boolean configureTask(@NotNull RunConfiguration runConfiguration, @NotNull T task) {
    final Artifact[] artifacts = ArtifactManager.getInstance(myProject).getArtifacts();
    Set<ArtifactPointer> pointers = new THashSet<>();
    for (Artifact artifact : artifacts) {
      pointers.add(ArtifactPointerManager.getInstance(myProject).createPointer(artifact));
    }
    pointers.addAll(task.getArtifactPointers());
    ArtifactChooser chooser = new ArtifactChooser(new ArrayList<>(pointers));
    chooser.markElements(task.getArtifactPointers());
    chooser.setPreferredSize(JBUI.size(400, 300));

    DialogBuilder builder = new DialogBuilder(myProject);
    builder.setTitle(CompilerBundle.message("build.artifacts.before.run.selector.title"));
    builder.setDimensionServiceKey("#BuildArtifactsBeforeRunChooser");
    builder.addOkAction();
    builder.addCancelAction();
    builder.setCenterPanel(chooser);
    builder.setPreferredFocusComponent(chooser);
    if (builder.show() == DialogWrapper.OK_EXIT_CODE) {
      task.setArtifactPointers(chooser.getMarkedElements());
      return true;
    }
    return false;
  }

  @Override
  public T createTask(@NotNull RunConfiguration runConfiguration) {
    if (myProject.isDefault()) return null;
    return doCreateTask(myProject);
  }

  @Override
  public boolean canExecuteTask(@NotNull RunConfiguration configuration, @NotNull T task) {
    for (ArtifactPointer pointer : (List<ArtifactPointer>)task.getArtifactPointers()) {
      if (pointer.getArtifact() != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean executeTask(@NotNull DataContext context,
                             @NotNull RunConfiguration configuration,
                             @NotNull final ExecutionEnvironment env,
                             @NotNull final T task) {
    final List<Artifact> artifacts = new ArrayList<>();
    ReadAction.run(() -> {
      List<ArtifactPointer> pointers = task.getArtifactPointers();
      for (ArtifactPointer pointer : pointers) {
        ContainerUtil.addIfNotNull(artifacts, pointer.getArtifact());
      }
    });

    if (myProject.isDisposed()) {
      return false;
    }
    ProjectTask artifactsBuildProjectTask = createProjectTask(myProject, artifacts);
    Object sessionId = ExecutionManagerImpl.EXECUTION_SESSION_ID_KEY.get(env);
    ProjectTaskContext projectTaskContext = new ProjectTaskContext(sessionId);
    env.copyUserDataTo(projectTaskContext);
    Promise<ProjectTaskManager.Result> resultPromise = ProjectTaskManager.getInstance(myProject)
      .run(projectTaskContext, artifactsBuildProjectTask);
    ProjectTaskManager.Result taskResult = ProjectTaskManagerImpl.waitForPromise(resultPromise);
    return taskResult != null && !taskResult.isAborted() && !taskResult.hasErrors();
  }

  protected void setBuildArtifactBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
                                                 @NotNull Artifact artifact,
                                                 final boolean enable) {
    final DataContext dataContext = DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
    final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext);
    if (editor != null) {
      List<T> tasks = ContainerUtil.findAll(editor.getStepsBeforeLaunch(), myTaskClass);
      if (enable && tasks.isEmpty()) {
        T task = doCreateTask(myProject);
        task.addArtifact(artifact);
        task.setEnabled(true);
        editor.addBeforeLaunchStep(task);
      }
      else {
        for (T task : tasks) {
          if (enable) {
            task.addArtifact(artifact);
            task.setEnabled(true);
          }
          else {
            task.removeArtifact(artifact);
            if (task.getArtifactPointers().isEmpty()) {
              task.setEnabled(false);
            }
          }
        }
      }
    }
  }

  protected abstract T doCreateTask(Project project);

  protected abstract ProjectTask createProjectTask(Project project, List<Artifact> artifacts);
}
