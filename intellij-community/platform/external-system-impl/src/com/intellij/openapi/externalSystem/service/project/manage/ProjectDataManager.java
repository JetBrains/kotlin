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
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @deprecated use {@link com.intellij.openapi.externalSystem.service.project.ProjectDataManager} instead
 */
@Deprecated
public class ProjectDataManager extends ProjectDataManagerImpl {

  public static ProjectDataManager getInstance() {
    return new ProjectDataManager(ProjectDataManagerImpl.getInstance());
  }

  private final ProjectDataManagerImpl delegate;

  public ProjectDataManager(ProjectDataManagerImpl delegate) {this.delegate = delegate;}

  @Override
  public void importData(@NotNull Collection<DataNode<?>> nodes,
                         @NotNull Project project,
                         @NotNull IdeModifiableModelsProvider modelsProvider,
                         boolean synchronous) {
    delegate.importData(nodes, project, modelsProvider, synchronous);
  }

  @Override
  public <T> void importData(@NotNull Collection<DataNode<T>> nodes,
                             @NotNull Project project, boolean synchronous) {
    delegate.importData(nodes, project, synchronous);
  }

  @Override
  public <T> void importData(@NotNull DataNode<T> node,
                             @NotNull Project project,
                             @NotNull IdeModifiableModelsProvider modelsProvider,
                             boolean synchronous) {
    delegate.importData(node, project, modelsProvider, synchronous);
  }

  @Override
  public <T> void importData(@NotNull DataNode<T> node,
                             @NotNull Project project, boolean synchronous) {
    delegate.importData(node, project, synchronous);
  }

  @Override
  public void ensureTheDataIsReadyToUse(@Nullable DataNode dataNode) {
    delegate.ensureTheDataIsReadyToUse(dataNode);
  }

  @Override
  public <E, I> void removeData(@NotNull Key<E> key,
                                @NotNull Collection<I> toRemove,
                                @NotNull Collection<DataNode<E>> toIgnore,
                                @NotNull ProjectData projectData,
                                @NotNull Project project,
                                @NotNull IdeModifiableModelsProvider modelsProvider,
                                boolean synchronous) {
    delegate.removeData(key, toRemove, toIgnore, projectData, project, modelsProvider, synchronous);
  }

  @Override
  public <E, I> void removeData(@NotNull Key<E> key,
                                @NotNull Collection<I> toRemove,
                                @NotNull Collection<DataNode<E>> toIgnore,
                                @NotNull ProjectData projectData,
                                @NotNull Project project, boolean synchronous) {
    delegate.removeData(key, toRemove, toIgnore, projectData, project, synchronous);
  }

  @Override
  public void updateExternalProjectData(@NotNull Project project,
                                        @NotNull ExternalProjectInfo externalProjectInfo) {
    delegate.updateExternalProjectData(project, externalProjectInfo);
  }

  @Nullable
  @Override
  public ExternalProjectInfo getExternalProjectData(@NotNull Project project,
                                                    @NotNull ProjectSystemId projectSystemId,
                                                    @NotNull String externalProjectPath) {
    return delegate.getExternalProjectData(project, projectSystemId, externalProjectPath);
  }

  @NotNull
  @Override
  public Collection<ExternalProjectInfo> getExternalProjectsData(@NotNull Project project,
                                                                 @NotNull ProjectSystemId projectSystemId) {
    return delegate.getExternalProjectsData(project, projectSystemId);
  }
}
