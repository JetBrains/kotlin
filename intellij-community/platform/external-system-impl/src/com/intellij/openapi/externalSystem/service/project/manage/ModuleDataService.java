/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates functionality of importing external system module to the intellij project.
 *
 * @author Denis Zhdanov
 */
@Order(ExternalSystemConstants.BUILTIN_MODULE_DATA_SERVICE_ORDER)
public class ModuleDataService extends AbstractModuleDataService<ModuleData> {

  @NotNull
  @Override
  public Key<ModuleData> getTargetDataKey() {
    return ProjectKeys.MODULE;
  }

  @NotNull
  @Override
  public Computable<Collection<Module>> computeOrphanData(@NotNull final Collection<DataNode<ModuleData>> toImport,
                                                          @NotNull final ProjectData projectData,
                                                          @NotNull final Project project,
                                                          @NotNull final IdeModifiableModelsProvider modelsProvider) {
    return () -> {
      List<Module> orphanIdeModules = ContainerUtil.newSmartList();

      for (Module module : modelsProvider.getModules()) {
        if (!ExternalSystemApiUtil.isExternalSystemAwareModule(projectData.getOwner(), module)) continue;
        if (ExternalSystemApiUtil.getExternalModuleType(module) != null) continue;

        final String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
        if (projectData.getLinkedExternalProjectPath().equals(rootProjectPath)) {
          if (module.getUserData(AbstractModuleDataService.MODULE_DATA_KEY) == null) {
            orphanIdeModules.add(module);
          }
        }
      }

      return orphanIdeModules;
    };
  }

  @Override
  public void postProcess(@NotNull Collection<DataNode<ModuleData>> toImport,
                          @Nullable ProjectData projectData,
                          @NotNull Project project,
                          @NotNull IdeModifiableModelsProvider modelsProvider) {
    super.postProcess(toImport, projectData, project, modelsProvider);

    updateLocalSettings(toImport, project);
  }

  private static void updateLocalSettings(Collection<DataNode<ModuleData>> toImport, Project project) {
    if (toImport.isEmpty()) {
      return;
    }
    ProjectSystemId externalSystemId = toImport.iterator().next().getData().getOwner();
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;

    final MultiMap<DataNode<ProjectData>, DataNode<ModuleData>> grouped = ExternalSystemApiUtil.groupBy(toImport, ProjectKeys.PROJECT);
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> data = ContainerUtilRt.newHashMap();
    for (Map.Entry<DataNode<ProjectData>, Collection<DataNode<ModuleData>>> entry : grouped.entrySet()) {
      data.put(ExternalProjectPojo.from(entry.getKey().getData()),
               ContainerUtilRt.map2List(entry.getValue(), node -> ExternalProjectPojo.from(node.getData())));
    }

    AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().fun(project);
    Set<String> pathsToForget = detectRenamedProjects(data, settings.getAvailableProjects());
    if (!pathsToForget.isEmpty()) {
      settings.forgetExternalProjects(pathsToForget);
    }
    Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects = ContainerUtilRt.newHashMap(settings.getAvailableProjects());
    projects.putAll(data);
    settings.setAvailableProjects(projects);
  }

  @NotNull
  private static Set<String> detectRenamedProjects(@NotNull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> currentInfo,
                                                   @NotNull Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> oldInfo) {
    Map<String/* external config path */, String/* project name */> map = ContainerUtilRt.newHashMap();
    for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : currentInfo.entrySet()) {
      map.put(entry.getKey().getPath(), entry.getKey().getName());
      for (ExternalProjectPojo pojo : entry.getValue()) {
        map.put(pojo.getPath(), pojo.getName());
      }
    }

    Set<String> result = ContainerUtilRt.newHashSet();
    for (Map.Entry<ExternalProjectPojo, Collection<ExternalProjectPojo>> entry : oldInfo.entrySet()) {
      String newName = map.get(entry.getKey().getPath());
      if (newName != null && !newName.equals(entry.getKey().getName())) {
        result.add(entry.getKey().getPath());
      }
      for (ExternalProjectPojo pojo : entry.getValue()) {
        newName = map.get(pojo.getPath());
        if (newName != null && !newName.equals(pojo.getName())) {
          result.add(pojo.getPath());
        }
      }
    }
    return result;
  }
}
