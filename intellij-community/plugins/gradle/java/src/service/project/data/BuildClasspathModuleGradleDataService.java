/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.plugins.gradle.service.project.data;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ExternalModuleBuildClasspathPojo;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectBuildClasspathPojo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData;
import org.jetbrains.plugins.gradle.service.GradleBuildClasspathManager;
import org.jetbrains.plugins.gradle.service.GradleInstallationManager;
import org.jetbrains.plugins.gradle.settings.GradleLocalSettings;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class BuildClasspathModuleGradleDataService extends AbstractProjectDataService<BuildScriptClasspathData, Module> {

  private static final Logger LOG = Logger.getInstance(BuildClasspathModuleGradleDataService.class);

  @NotNull
  @Override
  public Key<BuildScriptClasspathData> getTargetDataKey() {
    return BuildScriptClasspathData.KEY;
  }

  @Override
  public void importData(@NotNull final Collection<DataNode<BuildScriptClasspathData>> toImport,
                         @Nullable final ProjectData projectData,
                         @NotNull final Project project,
                         @NotNull final IdeModifiableModelsProvider modelsProvider) {
    if (projectData == null || toImport.isEmpty()) {
      return;
    }

    final GradleInstallationManager gradleInstallationManager = ServiceManager.getService(GradleInstallationManager.class);

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
    assert manager != null;
    AbstractExternalSystemLocalSettings<?> localSettings = manager.getLocalSettingsProvider().fun(project);

    final String linkedExternalProjectPath = projectData.getLinkedExternalProjectPath();
    final File gradleHomeDir = toImport.iterator().next().getData().getGradleHomeDir();
    final GradleLocalSettings gradleLocalSettings = GradleLocalSettings.getInstance(project);
    if (gradleHomeDir != null) {
      gradleLocalSettings.setGradleHome(linkedExternalProjectPath, gradleHomeDir.getPath());
    }
    final GradleProjectSettings settings = GradleSettings.getInstance(project).getLinkedProjectSettings(linkedExternalProjectPath);

    final NotNullLazyValue<Set<String>> externalProjectGradleSdkLibs = new NotNullLazyValue<Set<String>>() {
      @NotNull
      @Override
      protected Set<String> compute() {
        final Set<String> gradleSdkLibraries = ContainerUtil.newLinkedHashSet();
        File gradleHome = gradleInstallationManager.getGradleHome(project, linkedExternalProjectPath);
        if (gradleHome != null && gradleHome.isDirectory()) {
          final Collection<File> libraries = gradleInstallationManager.getClassRoots(project, linkedExternalProjectPath);
          if (libraries != null) {
            for (File library : libraries) {
              gradleSdkLibraries.add(FileUtil.toCanonicalPath(library.getPath()));
            }
          }
        }
        return gradleSdkLibraries;
      }
    };

    final Map<String, ExternalProjectBuildClasspathPojo> localProjectBuildClasspath = new THashMap<>(localSettings.getProjectBuildClasspath());
    for (final DataNode<BuildScriptClasspathData> node : toImport) {
      if (GradleConstants.SYSTEM_ID.equals(node.getData().getOwner())) {
        DataNode<ModuleData> moduleDataNode = ExternalSystemApiUtil.findParent(node, ProjectKeys.MODULE);
        if (moduleDataNode == null) continue;

        String externalModulePath = moduleDataNode.getData().getLinkedExternalProjectPath();
        if (settings == null || settings.getDistributionType() == null) {
          LOG.warn("Gradle SDK distribution type was not configured for the project at " + linkedExternalProjectPath);
        }

        final Set<String> buildClasspathSources = ContainerUtil.newLinkedHashSet();
        final Set<String> buildClasspathClasses = ContainerUtil.newLinkedHashSet();
        BuildScriptClasspathData buildScriptClasspathData = node.getData();
        for (BuildScriptClasspathData.ClasspathEntry classpathEntry : buildScriptClasspathData.getClasspathEntries()) {
          for (String path : classpathEntry.getSourcesFile()) {
            buildClasspathSources.add(FileUtil.toCanonicalPath(path));
          }

          for (String path : classpathEntry.getClassesFile()) {
            buildClasspathClasses.add(FileUtil.toCanonicalPath(path));
          }
        }

        ExternalProjectBuildClasspathPojo projectBuildClasspathPojo = localProjectBuildClasspath.get(linkedExternalProjectPath);
        if (projectBuildClasspathPojo == null) {
          projectBuildClasspathPojo = new ExternalProjectBuildClasspathPojo(
            moduleDataNode.getData().getExternalName(), ContainerUtil.newArrayList(), ContainerUtil.newHashMap());
          localProjectBuildClasspath.put(linkedExternalProjectPath, projectBuildClasspathPojo);
        }

        List<String> projectBuildClasspath = ContainerUtil.newArrayList(externalProjectGradleSdkLibs.getValue());

        projectBuildClasspathPojo.setProjectBuildClasspath(projectBuildClasspath);
        List<String> buildClasspath = StreamEx.of(buildClasspathSources).append(buildClasspathClasses).collect(Collectors.toList());
        projectBuildClasspathPojo.getModulesBuildClasspath().put(
          externalModulePath, new ExternalModuleBuildClasspathPojo(externalModulePath, buildClasspath));
      }
    }
    localSettings.setProjectBuildClasspath(localProjectBuildClasspath);

    if(!project.isDisposed()) {
      GradleBuildClasspathManager.getInstance(project).reload();
    }
  }
}
