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
package com.intellij.packaging.impl.run;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactPointer;
import com.intellij.task.ProjectTask;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class BuildArtifactsBeforeRunTaskProvider extends BuildArtifactsBeforeRunTaskProviderBase<BuildArtifactsBeforeRunTask> {
  @NonNls public static final String BUILD_ARTIFACTS_ID = "BuildArtifacts";
  public static final Key<BuildArtifactsBeforeRunTask> ID = Key.create(BUILD_ARTIFACTS_ID);

  public BuildArtifactsBeforeRunTaskProvider(Project project) {
    super(BuildArtifactsBeforeRunTask.class, project);
  }

  @Override
  public Key<BuildArtifactsBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public String getName() {
    return CompilerBundle.message("build.artifacts.before.run.description.empty");
  }

  @Override
  public Icon getTaskIcon(BuildArtifactsBeforeRunTask task) {
    List<ArtifactPointer> pointers = task.getArtifactPointers();
    if (pointers == null || pointers.isEmpty())
      return getIcon();
    Artifact artifact = pointers.get(0).getArtifact();
    if (artifact == null)
      return getIcon();
    return artifact.getArtifactType().getIcon();
  }

  @Override
  public String getDescription(BuildArtifactsBeforeRunTask task) {
    final List<ArtifactPointer> pointers = task.getArtifactPointers();
    if (pointers.isEmpty()) {
      return CompilerBundle.message("build.artifacts.before.run.description.empty");
    }
    if (pointers.size() == 1) {
      return CompilerBundle.message("build.artifacts.before.run.description.single", pointers.get(0).getArtifactName());
    }
    return CompilerBundle.message("build.artifacts.before.run.description.multiple", pointers.size());
  }

  public static void setBuildArtifactBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
                                                     Project project,
                                                     @NotNull Artifact artifact,
                                                     final boolean enable) {
    BeforeRunTaskProvider<BuildArtifactsBeforeRunTask> provider = getProvider(project, ID);
    if (provider != null) {
      ((BuildArtifactsBeforeRunTaskProvider)provider).setBuildArtifactBeforeRunOption(runConfigurationEditorComponent, artifact, enable);
    }
  }

  public static void setBuildArtifactBeforeRun(@NotNull Project project, @NotNull RunConfiguration configuration, @NotNull Artifact artifact) {
    RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
    final List<BuildArtifactsBeforeRunTask> buildArtifactsTasks = new ArrayList<>(runManager.getBeforeRunTasks(configuration, ID));
    if (runManager.getBeforeRunTasks(configuration, ID).isEmpty()) { //Add new task if absent
      BuildArtifactsBeforeRunTask task = new BuildArtifactsBeforeRunTask(project);
      buildArtifactsTasks.add(task);
      List<BeforeRunTask> tasks = new ArrayList<>(runManager.getBeforeRunTasks(configuration));
      tasks.add(task);
      runManager.setBeforeRunTasks(configuration, tasks);
    }

    for (BuildArtifactsBeforeRunTask task : buildArtifactsTasks) {
      task.setEnabled(true);
      task.addArtifact(artifact);
    }
  }

  @Override
  protected BuildArtifactsBeforeRunTask doCreateTask(Project project) {
    return new BuildArtifactsBeforeRunTask(project);
  }

  @Override
  protected ProjectTask createProjectTask(Project project, List<Artifact> artifacts) {
    return ProjectTaskManager.getInstance(project).createBuildTask(true, artifacts.toArray(new Artifact[0]));
  }
}
