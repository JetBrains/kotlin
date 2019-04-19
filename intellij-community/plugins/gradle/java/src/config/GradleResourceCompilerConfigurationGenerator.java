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
package org.jetbrains.plugins.gradle.config;

import com.intellij.ProjectTopics;
import com.intellij.compiler.server.BuildManager;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectType;
import com.intellij.openapi.project.ProjectTypeService;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.JdomKt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.xmlb.XmlSerializer;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gradle.model.impl.*;
import org.jetbrains.plugins.gradle.model.ExternalFilter;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceDirectorySet;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public class GradleResourceCompilerConfigurationGenerator {

  private static final Logger LOG = Logger.getInstance(GradleResourceCompilerConfigurationGenerator.class);

  @NotNull
  private final Project myProject;
  @NotNull
  private final Map<String, Integer> myModulesConfigurationHash;
  private final ExternalProjectDataCache externalProjectDataCache;

  public GradleResourceCompilerConfigurationGenerator(@NotNull final Project project) {
    myProject = project;
    myModulesConfigurationHash = ContainerUtil.newConcurrentMap();
    externalProjectDataCache = ExternalProjectDataCache.getInstance(project);
    assert externalProjectDataCache != null;

    project.getMessageBus().connect(project).subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
        myModulesConfigurationHash.remove(module.getName());
      }

      @Override
      public void modulesRenamed(@NotNull Project project,
                                 @NotNull List<Module> modules,
                                 @NotNull Function<Module, String> oldNameProvider) {
        for (Module module : modules) {
          moduleRemoved(project, module);
        }
      }
    });
  }

  public void generateBuildConfiguration(@NotNull final CompileContext context) {

    if (shouldBeBuiltByExternalSystem(myProject)) return;

    if (!hasGradleModules(context)) return;

    final BuildManager buildManager = BuildManager.getInstance();
    final File projectSystemDir = buildManager.getProjectSystemDirectory(myProject);
    if (projectSystemDir == null) return;

    final File gradleConfigFile = new File(projectSystemDir, GradleProjectConfiguration.CONFIGURATION_FILE_RELATIVE_PATH);

    final Map<String, GradleModuleResourceConfiguration> affectedGradleModuleConfigurations =
      generateAffectedGradleModulesConfiguration(context);

    if (affectedGradleModuleConfigurations.isEmpty()) return;

    boolean configurationUpdateRequired = context.isRebuild() || !gradleConfigFile.exists();

    final Map<String, Integer> affectedConfigurationHash = new THashMap<>();
    for (Map.Entry<String, GradleModuleResourceConfiguration> entry : affectedGradleModuleConfigurations.entrySet()) {
      Integer moduleLastConfigurationHash = myModulesConfigurationHash.get(entry.getKey());
      int moduleCurrentConfigurationHash = entry.getValue().computeConfigurationHash();
      if (moduleLastConfigurationHash == null || moduleLastConfigurationHash.intValue() != moduleCurrentConfigurationHash) {
        configurationUpdateRequired = true;
      }
      affectedConfigurationHash.put(entry.getKey(), moduleCurrentConfigurationHash);
    }

    final GradleProjectConfiguration projectConfig = loadLastConfiguration(gradleConfigFile);
    projectConfig.moduleConfigurations.putAll(affectedGradleModuleConfigurations);

    final Element element = new Element("gradle-project-configuration");
    XmlSerializer.serializeInto(projectConfig, element);
    final boolean finalConfigurationUpdateRequired = configurationUpdateRequired;
    buildManager.runCommand(() -> {
      if (finalConfigurationUpdateRequired) {
        buildManager.clearState(myProject);
      }
      FileUtil.createIfDoesntExist(gradleConfigFile);
      try {
        JdomKt.write(element, gradleConfigFile.toPath());
        myModulesConfigurationHash.putAll(affectedConfigurationHash);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @NotNull
  private GradleProjectConfiguration loadLastConfiguration(@NotNull File gradleConfigFile) {
    final GradleProjectConfiguration projectConfig = new GradleProjectConfiguration();
    if (gradleConfigFile.exists()) {
      try {
        XmlSerializer.deserializeInto(projectConfig, JDOMUtil.load(gradleConfigFile));

        // filter orphan modules
        final Set<String> actualModules = myModulesConfigurationHash.keySet();
        for (Iterator<Map.Entry<String, GradleModuleResourceConfiguration>> iterator =
             projectConfig.moduleConfigurations.entrySet().iterator(); iterator.hasNext(); ) {
          Map.Entry<String, GradleModuleResourceConfiguration> configurationEntry = iterator.next();
          if (!actualModules.contains(configurationEntry.getKey())) {
            iterator.remove();
          }
        }
      }
      catch (Exception e) {
        LOG.info(e);
      }
    }

    return projectConfig;
  }

  @NotNull
  private Map<String, GradleModuleResourceConfiguration> generateAffectedGradleModulesConfiguration(@NotNull CompileContext context) {
    final Map<String, GradleModuleResourceConfiguration> affectedGradleModuleConfigurations = ContainerUtil.newTroveMap();

    final Map<String, ExternalProject> lazyExternalProjectMap = FactoryMap.create(
      gradleProjectPath1 -> externalProjectDataCache.getRootExternalProject(gradleProjectPath1));

    for (Module module : context.getCompileScope().getAffectedModules()) {
      if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) continue;

      final String gradleProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
      assert gradleProjectPath != null;

      if (shouldBeBuiltByExternalSystem(module)) continue;

      final ExternalProject externalRootProject = lazyExternalProjectMap.get(gradleProjectPath);
      if (externalRootProject == null) {
        context.addMessage(CompilerMessageCategory.WARNING,
                           String.format("Unable to make the module: %s, related gradle configuration was not found. " +
                                         "Please, re-import the Gradle project and try again.",
                                         module.getName()), VfsUtilCore.pathToUrl(gradleProjectPath), -1, -1);
        continue;
      }

      Map<String, ExternalSourceSet> externalSourceSets = externalProjectDataCache.findExternalProject(externalRootProject, module);
      if (externalSourceSets.isEmpty()) {
        LOG.debug("Unable to find source sets config for module: " + module.getName());
        continue;
      }

      VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots(true);
      if (sourceRoots.length == 0) continue;

      GradleModuleResourceConfiguration resourceConfig = new GradleModuleResourceConfiguration();
      resourceConfig.id = new ModuleVersion(
        ExternalSystemApiUtil.getExternalProjectGroup(module),
        ExternalSystemApiUtil.getExternalProjectId(module),
        ExternalSystemApiUtil.getExternalProjectVersion(module));

      for (ExternalSourceSet sourceSet : externalSourceSets.values()) {
        addResources(resourceConfig.resources, sourceSet.getSources().get(ExternalSystemSourceType.RESOURCE),
                     sourceSet.getSources().get(ExternalSystemSourceType.SOURCE));
        addResources(resourceConfig.testResources, sourceSet.getSources().get(ExternalSystemSourceType.TEST_RESOURCE),
                     sourceSet.getSources().get(ExternalSystemSourceType.TEST));
      }

      final CompilerModuleExtension compilerModuleExtension = CompilerModuleExtension.getInstance(module);
      if (compilerModuleExtension != null && compilerModuleExtension.isCompilerOutputPathInherited()) {
        String outputPath = VfsUtilCore.urlToPath(compilerModuleExtension.getCompilerOutputUrl());
        for (ResourceRootConfiguration resource : resourceConfig.resources) {
          resource.targetPath = outputPath;
        }

        String testOutputPath = VfsUtilCore.urlToPath(compilerModuleExtension.getCompilerOutputUrlForTests());
        for (ResourceRootConfiguration resource : resourceConfig.testResources) {
          resource.targetPath = testOutputPath;
        }
      }

      affectedGradleModuleConfigurations.put(module.getName(), resourceConfig);
    }

    return affectedGradleModuleConfigurations;
  }

  private static boolean shouldBeBuiltByExternalSystem(@NotNull Project project) {
    // skip resource compilation by IDE for Android projects
    // TODO [vlad] this check should be replaced when an option to make any gradle project with gradle be introduced.
    ProjectType projectType = ProjectTypeService.getProjectType(project);
    if (projectType != null && "Android".equals(projectType.getId())) return true;
    return false;
  }

  private static boolean shouldBeBuiltByExternalSystem(@NotNull Module module) {
    for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
      if (ArrayUtil.contains(facet.getName(), "Android", "Android-Gradle", "Java-Gradle")) return true;
    }
    return false;
  }

  private static boolean hasGradleModules(@NotNull CompileContext context) {
    for (Module module : context.getCompileScope().getAffectedModules()) {
      if (ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return true;
    }
    return false;
  }

  private static void addResources(@NotNull List<ResourceRootConfiguration> container,
                                   @Nullable final ExternalSourceDirectorySet directorySet,
                                   @Nullable final ExternalSourceDirectorySet sourcesDirectorySet) {
    if (directorySet == null) return;

    for (File file : directorySet.getSrcDirs()) {
      final String dir = file.getPath();
      final ResourceRootConfiguration rootConfiguration = new ResourceRootConfiguration();
      rootConfiguration.directory = FileUtil.toSystemIndependentName(dir);
      final String target = directorySet.getOutputDir().getPath();
      rootConfiguration.targetPath = FileUtil.toSystemIndependentName(target);

      rootConfiguration.includes.clear();
      for (String include : directorySet.getPatterns().getIncludes()) {
        rootConfiguration.includes.add(include.trim());
      }
      rootConfiguration.excludes.clear();
      for (String exclude : directorySet.getPatterns().getExcludes()) {
        rootConfiguration.excludes.add(exclude.trim());
      }
      if (sourcesDirectorySet != null && sourcesDirectorySet.getSrcDirs().contains(file)) {
        rootConfiguration.excludes.add("**/*.java");
        rootConfiguration.excludes.add("**/*.scala");
        rootConfiguration.excludes.add("**/*.groovy");
        rootConfiguration.excludes.add("**/*.kt");
      }

      rootConfiguration.isFiltered = !directorySet.getFilters().isEmpty();
      rootConfiguration.filters.clear();
      for (ExternalFilter filter : directorySet.getFilters()) {
        final ResourceRootFilter resourceRootFilter = new ResourceRootFilter();
        resourceRootFilter.filterType = filter.getFilterType();
        resourceRootFilter.properties = filter.getPropertiesAsJsonMap();
        rootConfiguration.filters.add(resourceRootFilter);
      }

      container.add(rootConfiguration);
    }
  }
}
