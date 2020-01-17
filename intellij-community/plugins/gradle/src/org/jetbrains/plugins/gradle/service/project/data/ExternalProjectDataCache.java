// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.data;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.*;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vladislav.Soroka
 */
public class ExternalProjectDataCache {
  @ApiStatus.Internal
  @NotNull public static final Key<ExternalProject> KEY = Key.create(ExternalProject.class, ProjectKeys.TASK.getProcessingWeight() + 1);
  private final Project myProject;

  public static ExternalProjectDataCache getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ExternalProjectDataCache.class);
  }

  public ExternalProjectDataCache(@NotNull Project project) {
    myProject = project;
  }

  /**
   * Kept for compatibility with Kotlin
   * @deprecated since 2019.1
   */
  @Deprecated
  @Nullable
  public ExternalProject getRootExternalProject(@NotNull ProjectSystemId systemId, @NotNull File projectRootDir) {
    if (GradleConstants.SYSTEM_ID != systemId) {
      throw new IllegalStateException("Attempt to use Gradle-specific cache with illegal system id [" + systemId.getReadableName() + "]");
    }
    return getRootExternalProject(ExternalSystemApiUtil.toCanonicalPath(projectRootDir.getAbsolutePath()));
  }

  @Nullable
  public ExternalProject getRootExternalProject(@NotNull String externalProjectPath) {
    ExternalProjectInfo projectData =
      ProjectDataManager.getInstance().getExternalProjectData(myProject, GradleConstants.SYSTEM_ID, externalProjectPath);
    if (projectData == null) return null;
    DataNode<ProjectData> projectStructure = projectData.getExternalProjectStructure();
    if (projectStructure == null) return null;
    DataNode<ExternalProject> projectDataNode = ExternalSystemApiUtil.find(projectStructure, KEY);
    if (projectDataNode == null) return null;
    return projectDataNode.getData();
  }

  @NotNull
  public Map<String, ExternalSourceSet> findExternalProject(@NotNull ExternalProject parentProject, @NotNull Module module) {
    String externalProjectId = ExternalSystemApiUtil.getExternalProjectId(module);
    boolean isSourceSet = GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(ExternalSystemApiUtil.getExternalModuleType(module));
    return externalProjectId != null ? findExternalProject(parentProject, externalProjectId, isSourceSet)
                                     : Collections.emptyMap();
  }

  @NotNull
  private static Map<String, ExternalSourceSet> findExternalProject(@NotNull ExternalProject parentProject,
                                                                    @NotNull String externalProjectId,
                                                                    boolean isSourceSet) {
    ArrayDeque<ExternalProject> queue = new ArrayDeque<>();
    queue.add(parentProject);

    ExternalProject externalProject;
    while ((externalProject = queue.pollFirst()) != null) {
      final String projectId = externalProject.getId();
      boolean isRelatedProject = projectId.equals(externalProjectId);
      final Map<String, ExternalSourceSet> result = new HashMap<>();
      for (Map.Entry<String, ? extends ExternalSourceSet> sourceSetEntry : externalProject.getSourceSets().entrySet()) {
        final String sourceSetName = sourceSetEntry.getKey();
        final String sourceSetId = projectId + ":" + sourceSetName;
        if (isRelatedProject || (isSourceSet && externalProjectId.equals(sourceSetId))) {
          result.put(sourceSetName, sourceSetEntry.getValue());
        }
      }
      if(!result.isEmpty() || isRelatedProject) return result;

      queue.addAll(externalProject.getChildProjects().values());
    }

    return Collections.emptyMap();
  }


}
