// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.jps.gradle.model.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.*;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.cmdline.ProjectDescriptor;
import org.jetbrains.jps.gradle.model.JpsGradleExtensionService;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleResourcesTarget extends ModuleBasedTarget<GradleResourceRootDescriptor> {

  GradleResourcesTarget(final GradleResourcesTargetType type, @NotNull JpsModule module) {
    super(type, module);
  }

  @Override
  public String getId() {
    return myModule.getName();
  }

  @Override
  public Collection<BuildTarget<?>> computeDependencies(BuildTargetRegistry targetRegistry, TargetOutputIndex outputIndex) {
    return Collections.emptyList();
  }

  @Override
  public boolean isCompiledBeforeModuleLevelBuilders() {
    return true;
  }

  @NotNull
  @Override
  public List<GradleResourceRootDescriptor> computeRootDescriptors(JpsModel model, ModuleExcludeIndex index, IgnoredFileIndex ignoredFileIndex, BuildDataPaths dataPaths) {
    final List<GradleResourceRootDescriptor> result = new ArrayList<>();

    GradleProjectConfiguration projectConfig = JpsGradleExtensionService.getInstance().getGradleProjectConfiguration(dataPaths);
    GradleModuleResourceConfiguration moduleConfig = projectConfig.moduleConfigurations.get(myModule.getName());
    if (moduleConfig == null) return Collections.emptyList();

    int i = 0;

    for (ResourceRootConfiguration resource : getRootConfigurations(moduleConfig)) {
      result.add(new GradleResourceRootDescriptor(this, resource, i++, moduleConfig.overwrite));
    }
    return result;
  }

  private Collection<ResourceRootConfiguration> getRootConfigurations(@Nullable GradleModuleResourceConfiguration moduleConfig) {
    if (moduleConfig != null) {
      return isTests() ? moduleConfig.testResources : moduleConfig.resources;
    }
    return Collections.emptyList();
  }

  public GradleModuleResourceConfiguration getModuleResourcesConfiguration(BuildDataPaths dataPaths) {
    final GradleProjectConfiguration projectConfig = JpsGradleExtensionService.getInstance().getGradleProjectConfiguration(dataPaths);
    return projectConfig.moduleConfigurations.get(myModule.getName());
  }

  @Override
  public boolean isTests() {
    return ((GradleResourcesTargetType)getTargetType()).isTests();
  }

  @Nullable
  @Override
  public GradleResourceRootDescriptor findRootDescriptor(String rootId, BuildRootIndex rootIndex) {
    for (GradleResourceRootDescriptor descriptor : rootIndex.getTargetRoots(this, null)) {
      if (descriptor.getRootId().equals(rootId)) {
        return descriptor;
      }
    }
    return null;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return getTargetType().getTypeId() + ":" + myModule.getName();
  }

  @NotNull
  @Override
  public Collection<File> getOutputRoots(CompileContext context) {
    GradleModuleResourceConfiguration configuration =
      getModuleResourcesConfiguration(context.getProjectDescriptor().dataManager.getDataPaths());
    final Set<File> result = new THashSet<>(FileUtil.FILE_HASHING_STRATEGY);
    final File moduleOutput = getModuleOutputDir();
    for (ResourceRootConfiguration resConfig : getRootConfigurations(configuration)) {
      final File output = getOutputDir(moduleOutput, resConfig, configuration.outputDirectory);
      if (output != null) {
        result.add(output);
      }
    }
    return result;
  }

  @Nullable
  public File getModuleOutputDir() {
    return JpsJavaExtensionService.getInstance().getOutputDirectory(myModule, isTests());
  }

  @Nullable
  public static File getOutputDir(@Nullable File moduleOutput, ResourceRootConfiguration config, @Nullable String outputDirectory) {
    if(outputDirectory != null) {
      moduleOutput = JpsPathUtil.urlToFile(outputDirectory);
    }

    if (moduleOutput == null) {
      return null;
    }
    String targetPath = config.targetPath;
    if (StringUtil.isEmptyOrSpaces(targetPath)) {
      return moduleOutput;
    }
    final File targetPathFile = new File(targetPath);
    final File outputFile = targetPathFile.isAbsolute() ? targetPathFile : new File(moduleOutput, targetPath);
    return new File(FileUtil.toCanonicalPath(outputFile.getPath()));
  }

  @Override
  public void writeConfiguration(ProjectDescriptor pd, PrintWriter out) {
    final BuildDataPaths dataPaths = pd.getTargetsState().getDataPaths();
    final GradleModuleResourceConfiguration configuration = getModuleResourcesConfiguration(dataPaths);
    if (configuration != null) {
      out.write(Integer.toHexString(configuration.computeConfigurationHash(isTests())));
    }
  }
}
