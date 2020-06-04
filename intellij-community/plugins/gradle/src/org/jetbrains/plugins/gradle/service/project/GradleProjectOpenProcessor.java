// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.ide.GeneralSettings;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.AbstractWizard;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import icons.GradleIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.config.GradleSettingsListenerAdapter;
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

/**
 * @deprecated Use {@link org.jetbrains.plugins.gradle.service.project.open.GradleProjectOpenProcessor} instead
 *
 * @author Vladislav.Soroka
 */
@Deprecated
public final class GradleProjectOpenProcessor extends ProjectOpenProcessor {
  public static final String @NotNull [] BUILD_FILE_EXTENSIONS = {GradleConstants.EXTENSION, GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION};

  @NotNull
  @Override
  public String getName() {
    return GradleBundle.message("gradle.name");
  }

  @Override
  public @NotNull Icon getIcon() {
    return GradleIcons.Gradle;
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    if (file.isDirectory()) {
      return Arrays.stream(file.getChildren()).anyMatch(GradleProjectOpenProcessor::canOpenFile);
    }
    else {
      return canOpenFile(file);
    }
  }

  public static boolean canOpenFile(VirtualFile file) {
    return !file.isDirectory() && Arrays.stream(BUILD_FILE_EXTENSIONS).anyMatch(file.getName()::endsWith);
  }

  @Nullable
  @Override
  public Project doOpenProject(@NotNull VirtualFile virtualFile, @Nullable Project projectToClose, boolean forceOpenInNewFrame) {
    projectToClose = forceOpenInNewFrame ? null : projectToClose;
    Path path = Paths.get(virtualFile.getPath());
    return openGradleProject(null, projectToClose, path);
  }

  @Nullable
  public static Project openGradleProject(@Nullable Project projectToOpen,
                                          @Nullable Project projectToClose,
                                          @NotNull Path path) {
    GradleProjectOpenProcessor gradleProjectOpenProcessor =
      ProjectOpenProcessor.EXTENSION_POINT_NAME.findExtensionOrFail(GradleProjectOpenProcessor.class);
    VirtualFile virtualFile = VfsUtil.findFile(path, false);
    if (virtualFile != null && virtualFile.isDirectory()) {
      for (VirtualFile file : virtualFile.getChildren()) {
        if (gradleProjectOpenProcessor.canOpenProject(file)) {
          virtualFile = file;
          break;
        }
      }
    }

    String pathToOpen = virtualFile != null ? virtualFile.getParent().getPath() : path.toString();

    final WizardContext wizardContext = new WizardContext(null, null);
    wizardContext.setProjectFileDirectory(pathToOpen);
    GradleProjectSettings gradleProjectSettings = createDefaultProjectSettings();
    gradleProjectSettings.setExternalProjectPath(pathToOpen);
    boolean jvmFound = setupGradleJvm(ObjectUtils.chooseNotNull(projectToOpen, projectToClose), gradleProjectSettings);
    GradleAbstractWizard wizard = new GradleAbstractWizard(wizardContext, gradleProjectSettings);
    AbstractExternalModuleBuilder<GradleProjectSettings> wizardBuilder = wizard.getBuilder();
    try {
      if (!jvmFound) {
        wizard.show();
      }
      if (jvmFound || DialogWrapper.OK_EXIT_CODE == wizard.getExitCode()) {
        if (projectToOpen == null) {
          projectToOpen = ProjectManagerEx.getInstanceEx().newProject(Paths.get(pathToOpen).normalize(), OpenProjectTask.newProject().withProjectName(wizardContext.getProjectName()));
        }
        if (projectToOpen == null) return null;

        ExternalProjectsManagerImpl.getInstance(projectToOpen).setStoreExternally(true);
        VirtualFile finalVirtualFile = virtualFile;
        Project finalProjectToOpen = projectToOpen;
        ExternalSystemApiUtil.subscribe(projectToOpen, GradleConstants.SYSTEM_ID, new GradleSettingsListenerAdapter() {
          @Override
          public void onProjectsLinked(@NotNull Collection<GradleProjectSettings> settings) {
            createProjectPreview(finalProjectToOpen, pathToOpen, finalVirtualFile);
          }
        });
        wizardBuilder.commit(projectToOpen, null, ModulesProvider.EMPTY_MODULES_PROVIDER);
        projectToOpen.save();

        if (projectToClose != null) {
          closePreviousProject(projectToClose);
        }

        projectToOpen.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
        if (!projectToOpen.isOpen()) {
          ProjectManagerEx.getInstanceEx().openProject(projectToOpen);
        }
        return projectToOpen;
      }
    }
    finally {
      wizardBuilder.cleanup();
      Disposer.dispose(wizard.getDisposable());
    }
    return null;
  }

  public static void attachGradleProjectAndRefresh(@NotNull Project project, @NotNull String gradleProjectPath) {
    openGradleProject(project, null, Paths.get(gradleProjectPath));
  }

  @NotNull
  private static GradleProjectSettings createDefaultProjectSettings() {
    GradleProjectSettings settings = new GradleProjectSettings();
    settings.setupNewProjectDefault();
    settings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
    return settings;
  }


  private static void closePreviousProject(final Project projectToClose) {
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    if (openProjects.length > 0) {
      int exitCode = ProjectUtil.confirmOpenNewProject(true);
      if (exitCode == GeneralSettings.OPEN_PROJECT_SAME_WINDOW) {
        Project project = projectToClose != null ? projectToClose : openProjects[openProjects.length - 1];
        ProjectManagerEx.getInstanceEx().closeAndDispose(project);
      }
    }
  }

  private static class GradleAbstractWizard extends AbstractWizard<ExternalModuleSettingsStep> {
    private final AbstractExternalModuleBuilder<GradleProjectSettings> myBuilder;

    GradleAbstractWizard(WizardContext wizardContext, GradleProjectSettings gradleProjectSettings) {
      super("Open Gradle Project", (Project)null);
      myBuilder = new AbstractExternalModuleBuilder<GradleProjectSettings>(GradleConstants.SYSTEM_ID, gradleProjectSettings) {
        @Override
        protected void setupModule(Module module) throws ConfigurationException {
          super.setupModule(module);
          // it will be set later in any case, but save is called immediately after project creation, so, to ensure that it will be properly saved as external system module
          ExternalSystemModulePropertyManager.getInstance(module).setExternalId(GradleConstants.SYSTEM_ID);

          final Project project = module.getProject();
          FileDocumentManager.getInstance().saveAllDocuments();
          final GradleProjectSettings gradleProjectSettings = getExternalProjectSettings();
          attachGradleProjectAndRefresh(project, gradleProjectSettings);
        }

        @Override
        public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) {
          String contentEntryPath = getContentEntryPath();
          if (StringUtil.isEmpty(contentEntryPath)) {
            return;
          }
          File contentRootDir = new File(contentEntryPath);
          FileUtilRt.createDirectory(contentRootDir);
          LocalFileSystem fileSystem = LocalFileSystem.getInstance();
          VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(contentRootDir);
          if (modelContentRootDir == null) {
            return;
          }

          modifiableRootModel.addContentEntry(modelContentRootDir);
        }

        @Override
        public ModuleType getModuleType() {
          return ModuleTypeManager.getInstance().getDefaultModuleType();
        }
      };
      GradleProjectSettingsControl settingsControl = new GradleProjectSettingsControl(myBuilder.getExternalProjectSettings());
      ExternalModuleSettingsStep<GradleProjectSettings> step =
        new ExternalModuleSettingsStep<GradleProjectSettings>(wizardContext, myBuilder, settingsControl) {
          @Override
          public void _commit(boolean finishChosen) throws CommitStepException {
            try {
              validate();
              updateDataModel();
            }
            catch (ConfigurationException e) {
              throw new CommitStepException(e.getMessage());
            }
          }
        };
      addStep(step);
      init();
    }

    @Nullable
    @Override
    protected String getHelpID() {
      return null;
    }

    public AbstractExternalModuleBuilder<GradleProjectSettings> getBuilder() {
      return myBuilder;
    }
  }

  private static void attachGradleProjectAndRefresh(@NotNull Project project, @NotNull GradleProjectSettings gradleProjectSettings) {
    Runnable runnable = () -> {
      AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
      //noinspection unchecked
      settings.linkProject(gradleProjectSettings);

      ExternalSystemUtil.refreshProject(gradleProjectSettings.getExternalProjectPath(),
                                        new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID));
    };
    ExternalProjectsManagerImpl.getInstance(project)
      .runWhenInitialized(
        () -> DumbService.getInstance(project).runWhenSmart(
          () -> ExternalSystemUtil.ensureToolWindowInitialized(project, GradleConstants.SYSTEM_ID)));

    // execute when current dialog(if any) is closed
    ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL, runnable);
  }

  private static boolean setupGradleJvm(@Nullable Project project, @NotNull GradleProjectSettings projectSettings) {
    final Pair<String, Sdk> sdkPair = ExternalSystemJdkUtil.getAvailableJdk(project);
    if (!ExternalSystemJdkUtil.USE_INTERNAL_JAVA.equals(sdkPair.first) ||
        ExternalSystemJdkUtil.isValidJdk(sdkPair.second)) {
      projectSettings.setGradleJvm(sdkPair.first);
      return true;
    }

    String jdkPath = ContainerUtil.iterateAndGetLastItem(ExternalSystemJdkUtil.suggestJdkHomePaths());
    if (jdkPath != null) {
      Sdk sdk = ExternalSystemJdkUtil.addJdk(jdkPath);
      projectSettings.setGradleJvm(sdk.getName());
      return true;
    }
    return false;
  }

  private static void createProjectPreview(@NotNull Project project, @NotNull String rootProjectPath, @Nullable VirtualFile virtualFile) {
    ExternalSystemUtil.refreshProject(rootProjectPath,
                                      new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                                        .usePreviewMode()
                                        .use(ProgressExecutionMode.MODAL_SYNC));
    ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized(() -> DumbService.getInstance(project).runWhenSmart(() -> {
      ExternalSystemUtil.ensureToolWindowInitialized(project, GradleConstants.SYSTEM_ID);
      if (virtualFile == null) return;
      final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
      if (psiFile != null) {
        ProjectView.getInstance(project).selectPsiElement(psiFile, false);
      }
    }));
  }
}
