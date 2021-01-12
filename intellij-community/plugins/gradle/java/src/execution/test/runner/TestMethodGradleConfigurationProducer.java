// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.InheritorChooser;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleExecutionSettingsUtil;

import java.util.List;

import static org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.getMethodLocation;
import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.applyTestConfiguration;

/**
 * @author Vladislav.Soroka
 */
public class TestMethodGradleConfigurationProducer extends GradleTestRunConfigurationProducer {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return GradleExternalTaskConfigurationType.getInstance().getFactory();
  }

  @Override
  protected boolean doSetupConfigurationFromContext(ExternalSystemRunConfiguration configuration,
                                                    ConfigurationContext context,
                                                    Ref<PsiElement> sourceElement) {
    if (RunConfigurationProducer.getInstance(PatternGradleConfigurationProducer.class).isMultipleElementsSelected(context)) {
      return false;
    }
    final Location contextLocation = context.getLocation();
    assert contextLocation != null;
    PsiMethod psiMethod = getPsiMethodForLocation(contextLocation);
    if (psiMethod == null) return false;
    sourceElement.set(psiMethod);

    final PsiClass containingClass = psiMethod.getContainingClass();
    if (containingClass == null) return false;

    Module module = context.getModule();
    if (module == null) return false;

    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return false;
    if (!applyTestMethodConfiguration(configuration, context, psiMethod, containingClass)) return false;

    JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, contextLocation);
    return true;
  }

  @Nullable
  protected PsiMethod getPsiMethodForLocation(Location contextLocation) {
    Location<PsiMethod> location = getMethodLocation(contextLocation);
    return location != null ? location.getPsiElement() : null;
  }

  @Override
  protected boolean doIsConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context) {
    if (RunConfigurationProducer.getInstance(PatternGradleConfigurationProducer.class).isMultipleElementsSelected(context)) {
      return false;
    }

    final Location contextLocation = context.getLocation();
    assert contextLocation != null;

    PsiMethod psiMethod = getPsiMethodForLocation(contextLocation);
    if (psiMethod == null) return false;

    final PsiClass containingClass = psiMethod.getContainingClass();
    if (containingClass == null) return false;

    final Module module = context.getModule();
    if (module == null) return false;

    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return false;

    final String projectPath = resolveProjectPath(module);
    if (projectPath == null) return false;

    if (!StringUtil.equals(projectPath, configuration.getSettings().getExternalProjectPath())) {
      return false;
    }
    VirtualFile source = psiMethod.getContainingFile().getVirtualFile();
    if (!hasTasksInConfiguration(source, context.getProject(), configuration.getSettings())) return false;

    final String scriptParameters = configuration.getSettings().getScriptParameters() + ' ';
    final String testFilter = createTestFilter(contextLocation, containingClass, psiMethod);
    return testFilter != null && scriptParameters.contains(testFilter);
  }

  @Override
  public void onFirstRun(@NotNull final ConfigurationFromContext fromContext, @NotNull final ConfigurationContext context, @NotNull final Runnable performRunnable) {
    Runnable runnableWithCheck = addCheckForTemplateParams(fromContext, context, performRunnable);
    final PsiMethod psiMethod = (PsiMethod)fromContext.getSourceElement();
    final PsiClass psiClass = psiMethod.getContainingClass();
    final InheritorChooser inheritorChooser = new InheritorChooser() {
      @Override
      protected void runForClasses(List<PsiClass> classes, PsiMethod method, ConfigurationContext context, Runnable performRunnable) {
        chooseTestClassConfiguration(fromContext, context, performRunnable, psiMethod, classes.toArray(PsiClass.EMPTY_ARRAY));
      }

      @Override
      protected void runForClass(PsiClass aClass, PsiMethod psiMethod, ConfigurationContext context, Runnable performRunnable) {
        chooseTestClassConfiguration(fromContext, context, performRunnable, psiMethod, aClass);
      }
    };
    if (inheritorChooser.runMethodInAbstractClass(context, runnableWithCheck, psiMethod, psiClass)) return;
    chooseTestClassConfiguration(fromContext, context, runnableWithCheck, psiMethod, psiClass);
  }

  private void chooseTestClassConfiguration(@NotNull ConfigurationFromContext fromContext,
                                                   @NotNull ConfigurationContext context,
                                                   @NotNull Runnable performRunnable,
                                                   @NotNull PsiMethod psiMethod,
                                                   PsiClass @NotNull ... classes) {
    DataContext dataContext = TestTasksChooser.contextWithLocationName(context.getDataContext(), psiMethod.getName());
    getTestTasksChooser().chooseTestTasks(context.getProject(), dataContext, classes, tasks -> {
        ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration)fromContext.getConfiguration();
        ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
        Function1<PsiClass, String> createFilter = (psiClass) -> createTestFilter(context.getLocation(), psiClass, psiMethod);
        if (!applyTestConfiguration(settings, context.getModule(), tasks, classes, createFilter)) {
          LOG.warn("Cannot apply method test configuration, uses raw run configuration");
          performRunnable.run();
          return;
        }
        configuration.setName((classes.length == 1 ? classes[0].getName() + "." : "") + psiMethod.getName());
        performRunnable.run();
    });
  }

  private static boolean applyTestMethodConfiguration(@NotNull ExternalSystemRunConfiguration configuration,
                                                      @NotNull ConfigurationContext context,
                                                      @NotNull PsiMethod psiMethod,
                                                      PsiClass @NotNull ... containingClasses) {
    final ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
    final Function1<PsiClass, String> createFilter = (psiClass) -> createTestFilter(context.getLocation(), psiClass, psiMethod);
    if (!applyTestConfiguration(settings, context.getModule(), containingClasses, createFilter)) return false;
    configuration.setName((containingClasses.length == 1 ? containingClasses[0].getName() + "." : "") + psiMethod.getName());
    return true;
  }

  @Nullable
  private static String createTestFilter(@Nullable Location location, @NotNull PsiClass aClass, @NotNull PsiMethod psiMethod) {
    String filter = GradleExecutionSettingsUtil.createTestFilterFrom(location, aClass, psiMethod, true);
    return filter.isEmpty() ? null : filter;
  }

  @NotNull
  public static String createTestFilter(@Nullable String aClass, @Nullable String method) {
    return GradleExecutionSettingsUtil.createTestFilterFromMethod(aClass, method, /*hasSuffix=*/true);
  }
}
