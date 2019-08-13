// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.JavaRuntimeConfigurationProducerBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.TasksToRun;

import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.applyTestConfiguration;
import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.getSourceFile;
import static org.jetbrains.plugins.gradle.util.GradleExecutionSettingsUtil.createTestFilterFrom;

/**
 * @author Vladislav.Soroka
 */
public final class AllInPackageGradleConfigurationProducer extends GradleTestRunConfigurationProducer {
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

    TasksToRun tasksToRun = findTestsTaskToRun(configurationData.source, context.getProject());

    sourceElement.set(configurationData.sourceElement);

    configuration.getSettings().setExternalProjectPath(configurationData.projectPath);
    configuration.getSettings().setTaskNames(tasksToRun);
    String filter = createTestFilterFrom(configurationData.psiPackage, /*hasSuffix=*/false);
    configuration.getSettings().setScriptParameters(filter);
    configuration.setName(suggestName(configurationData));
    return true;
  }

  @Override
  protected boolean doIsConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context) {
    ConfigurationData configurationData = extractConfigurationData(context);
    if (configurationData == null) return false;
    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, configurationData.module)) return false;

    if (!StringUtil.equals(
      configurationData.projectPath,
      configuration.getSettings().getExternalProjectPath())) {
      return false;
    }
    if (!hasTasksInConfiguration(configurationData.source, context.getProject(), configuration.getSettings())) return false;

    final String scriptParameters = configuration.getSettings().getScriptParameters() + ' ';
    final String filter = createTestFilterFrom(configurationData.psiPackage, /*hasSuffix=*/true);
    return scriptParameters.contains(filter);
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
    String locationName = String.format("'%s'", configurationData.getLocationName());
    DataContext dataContext = TestTasksChooser.contextWithLocationName(context.getDataContext(), locationName);
    PsiElement[] sourceElements = ArrayUtil.toObjectArray(PsiElement.class, configurationData.sourceElement);
    getTestTasksChooser().chooseTestTasks(context.getProject(), dataContext, sourceElements, tasks -> {
        ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration)fromContext.getConfiguration();
        ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
        Function1<PsiElement, String> createFilter = (e) -> createTestFilterFrom(configurationData.psiPackage, /*hasSuffix=*/false);
        if (!applyTestConfiguration(settings, context.getModule(), tasks, sourceElements, createFilter)) {
          LOG.warn("Cannot apply package test configuration, uses raw run configuration");
          performRunnable.run();
          return;
        }
      configuration.setName(suggestName(configurationData));
        performRunnable.run();
    });
  }

  @Nullable
  private ConfigurationData extractConfigurationData(ConfigurationContext context) {
    Module module = context.getModule();
    if (module == null) return null;
    PsiElement contextLocation = context.getPsiLocation();
    if (contextLocation == null) return null;
    PsiPackage psiPackage = extractPackage(contextLocation);
    if (psiPackage == null) return null;
    String projectPath = resolveProjectPath(module);
    if (projectPath == null) return null;
    PsiElement sourceElement = getSourceElement(module, contextLocation);
    if (sourceElement == null) return null;
    VirtualFile source = getSourceFile(sourceElement);
    if (source == null) return null;
    return new ConfigurationData(module, psiPackage, sourceElement, source, projectPath);
  }

  @Nullable
  private static PsiElement getSourceElement(@NotNull Module module, @NotNull PsiElement element) {
    if (element instanceof PsiFileSystemItem) {
      return element;
    }
    PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      return element;
    }
    if (element instanceof PsiPackage) {
      return getPackageDirectory(module, (PsiPackage)element);
    }
    return null;
  }

  @Nullable
  private static PsiDirectory getPackageDirectory(@NotNull Module module, @NotNull PsiPackage element) {
    PsiDirectory[] sourceDirs = element.getDirectories(GlobalSearchScope.moduleScope(module));
    if (sourceDirs.length == 0) return null;
    return sourceDirs[0];
  }

  @Nullable
  static PsiPackage extractPackage(@NotNull PsiElement location) {
    PsiPackage psiPackage = JavaRuntimeConfigurationProducerBase.checkPackage(location);
    if (psiPackage == null) return null;
    if (psiPackage.getQualifiedName().isEmpty()) return null;
    return psiPackage;
  }

  @NotNull
  private static String suggestName(@NotNull ConfigurationData configurationData) {
    return ExecutionBundle.message("test.in.scope.presentable.text", configurationData.getLocationName());
  }

  private static class ConfigurationData {
    public final @NotNull Module module;
    public final @NotNull PsiPackage psiPackage;
    public final @NotNull PsiElement sourceElement;
    public final @NotNull VirtualFile source;
    public final @NotNull String projectPath;

    private ConfigurationData(@NotNull Module module,
                              @NotNull PsiPackage psiPackage,
                              @NotNull PsiElement sourceElement,
                              @NotNull VirtualFile source,
                              @NotNull String projectPath) {
      this.module = module;
      this.psiPackage = psiPackage;
      this.sourceElement = sourceElement;
      this.source = source;
      this.projectPath = projectPath;
    }

    @NotNull
    public String getLocationName() {
      return psiPackage.getQualifiedName();
    }
  }
}
