// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.codeInsight.TestFrameworks;
import com.intellij.execution.JavaExecutionUtil;
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
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.containers.ContainerUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.TasksToRun;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.getMethodLocation;
import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.applyTestConfiguration;
import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.escapeIfNeeded;
import static org.jetbrains.plugins.gradle.util.GradleExecutionSettingsUtil.createTestFilterFrom;

/**
 * @author Vladislav.Soroka
 */
public class TestClassGradleConfigurationProducer extends GradleTestRunConfigurationProducer {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return GradleExternalTaskConfigurationType.getInstance().getFactory();
  }

  @Override
  protected boolean doSetupConfigurationFromContext(ExternalSystemRunConfiguration configuration,
                                                    ConfigurationContext context,
                                                    Ref<PsiElement> sourceElement) {
    final Location contextLocation = context.getLocation();
    assert contextLocation != null;

    if (RunConfigurationProducer.getInstance(PatternGradleConfigurationProducer.class).isMultipleElementsSelected(context)) {
      return false;
    }
    PsiClass testClass = getPsiClassForLocation(contextLocation);
    if (testClass == null) {
      return false;
    }
    sourceElement.set(testClass);

    final Module module = context.getModule();
    if (module == null) return false;

    if (!ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return false;

    final String projectPath = resolveProjectPath(module);
    if (projectPath == null) return false;

    VirtualFile source = testClass.getContainingFile().getVirtualFile();
    TasksToRun tasksToRun = findTestsTaskToRun(source, context.getProject());

    configuration.getSettings().setExternalProjectPath(projectPath);
    configuration.getSettings().setTaskNames(ContainerUtil.map(tasksToRun, it -> escapeIfNeeded(it)));

    String filter = createTestFilterFrom(testClass, /*hasSuffix=*/false);
    configuration.getSettings().setScriptParameters(filter);
    configuration.setName(testClass.getName());

    JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, contextLocation);
    return true;
  }

  @Nullable
  protected PsiMethod getPsiMethodForLocation(Location contextLocation) {
    Location<PsiMethod> location = getMethodLocation(contextLocation);
    return location != null ? location.getPsiElement() : null;
  }

  @Nullable
  protected PsiClass getPsiClassForLocation(Location contextLocation) {
    final Location<?> location = JavaExecutionUtil.stepIntoSingleClass(contextLocation);
    if (location == null) return null;

    TestFrameworks testFrameworks = TestFrameworks.getInstance();
    for (Iterator<Location<PsiClass>> iterator = location.getAncestors(PsiClass.class, false); iterator.hasNext(); ) {
      final Location<PsiClass> classLocation = iterator.next();
      if (testFrameworks.isTestClass(classLocation.getPsiElement())) return classLocation.getPsiElement();
    }
    PsiElement element = location.getPsiElement();
    if (element instanceof PsiClassOwner) {
      PsiClass[] classes = ((PsiClassOwner)element).getClasses();
      if (classes.length == 1 && testFrameworks.isTestClass(classes[0])) return classes[0];
    }
    return null;
  }

  @Override
  protected boolean doIsConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context) {
    final Location contextLocation = context.getLocation();
    assert contextLocation != null;

    if (RunConfigurationProducer.getInstance(PatternGradleConfigurationProducer.class).isMultipleElementsSelected(context)) {
      return false;
    }

    if (getPsiMethodForLocation(contextLocation) != null) return false;
    PsiClass testClass = getPsiClassForLocation(contextLocation);
    if (testClass == null || testClass.getQualifiedName() == null) return false;

    if (context.getModule() == null) return false;

    final String projectPath = resolveProjectPath(context.getModule());
    if (projectPath == null) return false;
    if (!StringUtil.equals(projectPath, configuration.getSettings().getExternalProjectPath())) {
      return false;
    }
    VirtualFile source = testClass.getContainingFile().getVirtualFile();
    if (!hasTasksInConfiguration(source, context.getProject(), configuration.getSettings())) return false;

    final String scriptParameters = configuration.getSettings().getScriptParameters() + ' ';
    int i = scriptParameters.indexOf("--tests ");
    if(i == -1) return false;

    String testFilter = createTestFilterFrom(testClass, /*hasSuffix=*/true);
    String filter = testFilter.substring("--tests ".length());
    String str = scriptParameters.substring(i + "--tests ".length()).trim() + ' ';
    return str.startsWith(filter) && !str.contains("--tests");
  }

  @Override
  public void onFirstRun(@NotNull final ConfigurationFromContext fromContext, @NotNull final ConfigurationContext context, @NotNull final Runnable performRunnable) {
    Runnable runnableWithCheck = addCheckForTemplateParams(fromContext, context, performRunnable);
    final InheritorChooser inheritorChooser = new InheritorChooser() {
      @Override
      protected void runForClasses(List<PsiClass> classes, PsiMethod method, ConfigurationContext context, Runnable performRunnable) {
        chooseTestClassConfiguration(fromContext, context, performRunnable, classes.toArray(PsiClass.EMPTY_ARRAY));
      }

      @Override
      protected void runForClass(PsiClass aClass, PsiMethod psiMethod, ConfigurationContext context, Runnable performRunnable) {
        chooseTestClassConfiguration(fromContext, context, performRunnable, aClass);
      }
    };
    if (inheritorChooser.runMethodInAbstractClass(context, runnableWithCheck, null, (PsiClass)fromContext.getSourceElement())) return;
    PsiClass psiClass = (PsiClass)fromContext.getSourceElement();
    chooseTestClassConfiguration(fromContext, context, runnableWithCheck, psiClass);
  }

  private void chooseTestClassConfiguration(@NotNull ConfigurationFromContext fromContext,
                                                   @NotNull ConfigurationContext context,
                                                   @NotNull Runnable performRunnable,
                                                   PsiClass @NotNull ... classes) {
    String locationName = classes.length == 1 ? classes[0].getName() : null;
    DataContext dataContext = TestTasksChooser.contextWithLocationName(context.getDataContext(), locationName);
    getTestTasksChooser().chooseTestTasks(context.getProject(), dataContext, classes, tasks -> {
        ExternalSystemRunConfiguration configuration = (ExternalSystemRunConfiguration)fromContext.getConfiguration();
        ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
        Function1<PsiClass, String> createFilter = (psiClass) -> createTestFilterFrom(psiClass, /*hasSuffix=*/true);
        if (!applyTestConfiguration(settings, context.getModule(), tasks, classes, createFilter)) {
          LOG.warn("Cannot apply class test configuration, uses raw run configuration");
          performRunnable.run();
          return;
        }
        configuration.setName(StringUtil.join(classes, aClass -> aClass.getName(), "|"));
        performRunnable.run();
    });
  }
}
