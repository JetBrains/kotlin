// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.project.wizard.SelectExternalProjectStep;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectOpenProcessorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor;
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl;
import org.jetbrains.plugins.gradle.service.settings.GradleSystemSettingsControl;
import org.jetbrains.plugins.gradle.service.settings.ImportFromGradleControl;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import static org.jetbrains.plugins.gradle.util.GradleEnvironment.Headless.*;

/**
 * @deprecated Use {@link org.jetbrains.plugins.gradle.service.project.open.GradleProjectOpenProcessor} instead
 *
 * @author Denis Zhdanov
 */
@Deprecated
public class GradleJavaProjectOpenProcessor extends ProjectOpenProcessorBase<GradleProjectImportBuilder> {
  @NotNull
  @Override
  protected GradleProjectImportBuilder doGetBuilder() {
    return ProjectImportBuilder.EXTENSIONS_POINT_NAME.findExtensionOrFail(GradleProjectImportBuilder.class);
  }

  @NotNull
  @Override
  public String[] getSupportedExtensions() {
    return new String[] {GradleConstants.DEFAULT_SCRIPT_NAME, GradleConstants.SETTINGS_FILE_NAME, GradleConstants.KOTLIN_DSL_SCRIPT_NAME};
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    if (GradleProjectOpenProcessor.canOpenFile(file)) {
      return true;
    }
    return super.canOpenProject(file);
  }

  @Override
  protected boolean doQuickImport(@NotNull VirtualFile file, @NotNull WizardContext wizardContext) {
    final GradleProjectImportProvider projectImportProvider = new GradleProjectImportProvider(getBuilder());
    getBuilder().setFileToImport(file.getPath());
    getBuilder().prepare(wizardContext);

    final String pathToUse;
    if (!file.isDirectory() && file.getParent() != null) {
      pathToUse = file.getParent().getPath();
    }
    else {
      pathToUse = file.getPath();
    }
    getBuilder().getControl(null).setLinkedProjectPath(pathToUse);

    final boolean result;
    WizardContext dialogWizardContext = null;
    if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
      result = setupGradleProjectSettingsInHeadlessMode(projectImportProvider, wizardContext);
    }
    else {
      AddModuleWizard dialog = new AddModuleWizard(null, file.getPath(), projectImportProvider);
      dialogWizardContext = dialog.getWizardContext();
      dialogWizardContext.setProjectBuilder(getBuilder());
      dialog.navigateToStep(step -> step instanceof SelectExternalProjectStep);
      result = dialog.showAndGet();
    }
    if (result && getBuilder().getExternalProjectNode() != null) {
      wizardContext.setProjectName(getBuilder().getExternalProjectNode().getData().getInternalName());
    }
    if(result && dialogWizardContext != null) {
      wizardContext.setProjectStorageFormat(dialogWizardContext.getProjectStorageFormat());
    }
    return result;
  }

  private boolean setupGradleProjectSettingsInHeadlessMode(GradleProjectImportProvider projectImportProvider,
                                                           WizardContext wizardContext) {
    final ModuleWizardStep[] wizardSteps = projectImportProvider.createSteps(wizardContext);
    if (wizardSteps.length > 0 && wizardSteps[0] instanceof SelectExternalProjectStep) {
      SelectExternalProjectStep selectExternalProjectStep = (SelectExternalProjectStep)wizardSteps[0];
      wizardContext.setProjectBuilder(getBuilder());
      try {
        selectExternalProjectStep.updateStep();
        final ImportFromGradleControl importFromGradleControl = getBuilder().getControl(wizardContext.getProject());

        GradleProjectSettingsControl gradleProjectSettingsControl =
          (GradleProjectSettingsControl)importFromGradleControl.getProjectSettingsControl();

        final GradleProjectSettings projectSettings = gradleProjectSettingsControl.getInitialSettings();

        if (GRADLE_DISTRIBUTION_TYPE != null) {
          for (DistributionType type : DistributionType.values()) {
            if (type.name().equals(GRADLE_DISTRIBUTION_TYPE)) {
              projectSettings.setDistributionType(type);
              break;
            }
          }
        }
        if (GRADLE_HOME != null) {
          projectSettings.setGradleHome(GRADLE_HOME);
        }
        gradleProjectSettingsControl.reset();

        final GradleSystemSettingsControl systemSettingsControl =
          (GradleSystemSettingsControl)importFromGradleControl.getSystemSettingsControl();
        assert systemSettingsControl != null;
        final GradleSettings gradleSettings = systemSettingsControl.getInitialSettings();
        if (GRADLE_VM_OPTIONS != null) {
          gradleSettings.setGradleVmOptions(GRADLE_VM_OPTIONS);
        }
        if (GRADLE_OFFLINE != null) {
          gradleSettings.setOfflineWork(Boolean.parseBoolean(GRADLE_OFFLINE));
        }
        if (GRADLE_SERVICE_DIRECTORY != null) {
          gradleSettings.setServiceDirectoryPath(GRADLE_SERVICE_DIRECTORY);
        }
        systemSettingsControl.reset();

        if (!selectExternalProjectStep.validate()) {
          return false;
        }
      }
      catch (ConfigurationException e) {
        Messages.showErrorDialog(wizardContext.getProject(), e.getMessage(), e.getTitle());
        return false;
      }
      selectExternalProjectStep.updateDataModel();
    }
    return true;
  }

  @Override
  public boolean isStrongProjectInfoHolder() {
    return ApplicationManager.getApplication().isHeadlessEnvironment();
  }
}
