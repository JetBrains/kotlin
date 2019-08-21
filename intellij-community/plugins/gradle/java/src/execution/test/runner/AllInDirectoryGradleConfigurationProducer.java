// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleUtil;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.applyTestConfiguration;
import static org.jetbrains.plugins.gradle.util.GradleExecutionSettingsUtil.createTestWildcardFilter;

public final class AllInDirectoryGradleConfigurationProducer extends GradleTestRunConfigurationProducer {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return GradleExternalTaskConfigurationType.getInstance().getFactory();
  }

  @Override
  protected boolean doSetupConfigurationFromContext(ExternalSystemRunConfiguration configuration,
                                                    ConfigurationContext context,
                                                    Ref<PsiElement> sourceElement) {
    ConfigurationData configurationData = extractConfigurationData(context);
    if (configurationData == null) return false;
    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, configurationData.module)) return false;
    sourceElement.set(configurationData.sourceElement);
    ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
    Function1<VirtualFile, String> createFilter = (e) -> createTestWildcardFilter(/*hasSuffix=*/false);
    if (!applyTestConfiguration(settings, context.getModule(), configurationData.sources, it -> it, createFilter)) {
      return false;
    }
    configuration.setName(suggestName(configurationData.module));
    return true;
  }

  @Override
  protected boolean doIsConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context) {
    ConfigurationData configurationData = extractConfigurationData(context);
    if (configurationData == null) return false;
    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, configurationData.module)) return false;
    String projectPath = configuration.getSettings().getExternalProjectPath();
    if (!StringUtil.equals(configurationData.projectPath, projectPath)) return false;
    if (configurationData.sources.isEmpty()) return false;
    for (VirtualFile source : configurationData.sources) {
      if (!hasTasksInConfiguration(source, context.getProject(), configuration.getSettings())) return false;
    }
    final String scriptParameters = configuration.getSettings().getScriptParameters() + ' ';
    return scriptParameters.contains(createTestWildcardFilter(/*hasSuffix=*/true));
  }

  @Override
  public void onFirstRun(@NotNull ConfigurationFromContext fromContext,
                         @NotNull ConfigurationContext context,
                         @NotNull Runnable performRunnable) {
    ConfigurationData configurationData = extractConfigurationData(context);
    if (configurationData == null) {
      LOG.warn("Cannot extract configuration data from context, uses raw run configuration");
      performRunnable.run();
      return;
    }
    String locationName = String.format("'%s'", configurationData.module.getName());
    DataContext dataContext = TestTasksChooser.contextWithLocationName(context.getDataContext(), locationName);
    getTestTasksChooser().chooseTestTasks(context.getProject(), dataContext, configurationData.sources, tasks -> {
      ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration)fromContext.getConfiguration();
      ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
      Function1<VirtualFile, String> createFilter = (e) -> createTestWildcardFilter(/*hasSuffix=*/false);
      if (!applyTestConfiguration(settings, context.getModule(), tasks, configurationData.sources, it -> it, createFilter)) {
        LOG.warn("Cannot apply package test configuration, uses raw run configuration");
        performRunnable.run();
        return;
      }
      configuration.setName(suggestName(configurationData.module));
      performRunnable.run();
    });
  }

  @Nullable
  private ConfigurationData extractConfigurationData(ConfigurationContext context) {
    Module module = context.getModule();
    if (module == null) return null;
    String projectPath = resolveProjectPath(module);
    if (projectPath == null) return null;
    PsiElement contextLocation = context.getPsiLocation();
    if (contextLocation == null) return null;
    if (AllInPackageGradleConfigurationProducer.extractPackage(contextLocation) != null) return null;
    if (!(contextLocation instanceof PsiFileSystemItem)) return null;
    PsiFileSystemItem directory = (PsiFileSystemItem)contextLocation;
    if (!directory.isDirectory()) return null;
    List<VirtualFile> sources = findTestSourcesUnderDirectory(module, directory.getVirtualFile());
    return new ConfigurationData(module, directory, sources, projectPath);
  }

  private static List<VirtualFile> findTestSourcesUnderDirectory(@NotNull Module module, @NotNull VirtualFile directory) {
    DataNode<ModuleData> moduleDataNode = GradleUtil.findGradleModuleData(module);
    if (moduleDataNode == null) return Collections.emptyList();
    String rootPath = directory.getPath();
    return ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.TEST).stream()
      .map(DataNode::getData)
      .flatMap(it -> it.getSourceFolders().stream())
      .filter(it -> FileUtil.isAncestor(rootPath, it, false))
      .map(it -> VfsUtil.findFile(Paths.get(it), false))
      .filter(Objects::nonNull)
      .distinct()
      .collect(Collectors.toList());
  }

  private static String suggestName(@NotNull Module module) {
    return ExecutionBundle.message("test.in.scope.presentable.text", module.getName());
  }

  private static class ConfigurationData {
    public final @NotNull Module module;
    public final @NotNull PsiElement sourceElement;
    public final @NotNull List<VirtualFile> sources;
    public final @NotNull String projectPath;

    private ConfigurationData(@NotNull Module module,
                              @NotNull PsiElement sourceElement,
                              @NotNull List<VirtualFile> sources,
                              @NotNull String projectPath) {
      this.module = module;
      this.sourceElement = sourceElement;
      this.sources = sources;
      this.projectPath = projectPath;
    }
  }
}
