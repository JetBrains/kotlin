// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class CachedModuleDataFinder {
  private final Map<String, DataNode<? extends ModuleData>> cache = new HashMap<>();

  @Nullable
  public DataNode<? extends ModuleData> findModuleData(final DataNode<ProjectData> projectNode, final String projectPath) {
    DataNode<? extends ModuleData> cachedNode = cache.get(projectPath);
    if (cachedNode != null) return cachedNode;

    return ExternalSystemApiUtil.find(projectNode, ProjectKeys.MODULE, node -> {
      String externalProjectPath = node.getData().getLinkedExternalProjectPath();
      cache.put(externalProjectPath, node);
      return StringUtil.equals(projectPath, node.getData().getLinkedExternalProjectPath());
    });
  }

  @Nullable
  public DataNode<? extends ModuleData> findModuleData(@NotNull Module module) {
    DataNode<? extends ModuleData> mainModuleData = findMainModuleData(module);
    if (mainModuleData == null) return null;

    boolean isSourceSet = GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(ExternalSystemApiUtil.getExternalModuleType(module));
    if (!isSourceSet) {
      return mainModuleData;
    }
    else {
      String projectId = ExternalSystemApiUtil.getExternalProjectId(module);
      DataNode<? extends ModuleData> cachedNode = cache.get(projectId);
      if (cachedNode != null) return cachedNode;

      return ExternalSystemApiUtil.find(mainModuleData, GradleSourceSetData.KEY, node -> {
        String id = node.getData().getId();
        cache.put(id, node);
        return StringUtil.equals(projectId, id);
      });
    }
  }

  @Nullable
  public DataNode<? extends ModuleData> findMainModuleData(@NotNull Module module) {
    final String rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    if (rootProjectPath == null) return null;

    final String projectId = ExternalSystemApiUtil.getExternalProjectId(module);
    if (projectId == null) return null;
    final String externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    if (externalProjectPath == null || StringUtil.endsWith(externalProjectPath, "buildSrc")) return null;

    ExternalProjectInfo projectData =
      ProjectDataManager.getInstance().getExternalProjectData(module.getProject(), GradleConstants.SYSTEM_ID, rootProjectPath);
    if (projectData == null) return null;

    DataNode<ProjectData> projectStructure = projectData.getExternalProjectStructure();
    if (projectStructure == null) return null;

    return findModuleData(projectStructure, externalProjectPath);
  }
}
