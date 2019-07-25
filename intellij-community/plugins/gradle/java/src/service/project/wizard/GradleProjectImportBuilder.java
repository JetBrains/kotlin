// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.externalSystem.JavaProjectData;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportBuilder;
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectDataSelectorDialog;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.ObjectUtils;
import gnu.trove.THashSet;
import icons.GradleIcons;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.settings.ImportFromGradleControl;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleBundle;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @deprecated Use {@link JavaGradleProjectImportBuilder} instead
 */
@Deprecated
public final class GradleProjectImportBuilder extends AbstractExternalProjectImportBuilder<ImportFromGradleControl> {
  private static final Logger LOG = Logger.getInstance(GradleProjectImportBuilder.class);

  public GradleProjectImportBuilder() {
    this(ProjectDataManager.getInstance());
  }

  /**
   * @deprecated use {@link GradleProjectImportBuilder#GradleProjectImportBuilder(ProjectDataManager)}
   */
  @Deprecated
  public GradleProjectImportBuilder(@NotNull com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager dataManager) {
    this((ProjectDataManager)dataManager);
  }

  public GradleProjectImportBuilder(@NotNull ProjectDataManager dataManager) {
    super(dataManager, () -> new ImportFromGradleControl(), GradleConstants.SYSTEM_ID);
    LOG.warn("Do not use `GradleProjectImportBuilder` directly. Use instead:\n" +
             "Internal stable Api\n" +
             " Use `com.intellij.ide.actions.ImportModuleAction.doImport` to import (attach) a new project.\n" +
             " Use `com.intellij.ide.impl.ProjectUtil.openOrImport` to open (import) a new project.\n" +
             "Internal experimental Api\n" +
             " Use `org.jetbrains.plugins.gradle.service.project.open.openProject` to open (import) a new gradle project.\n" +
             " Use `org.jetbrains.plugins.gradle.service.project.open.importProject` to attach a gradle project to an opened idea project.",
             new Throwable());
  }

  @NotNull
  @Override
  public String getName() {
    return GradleBundle.message("gradle.name");
  }

  @Override
  public Icon getIcon() {
    return GradleIcons.Gradle;
  }

  @Nullable
  @Override
  protected Sdk resolveProjectJdk(@NotNull WizardContext context) {
    JavaSdk javaSdkType = JavaSdk.getInstance();
    ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();

    // gradle older than 4.2.1 doesn't support new java the version number format like 9.0.1, see https://github.com/gradle/gradle/issues/2992
    Predicate<Sdk> sdkCondition = sdk -> {
      JavaSdkVersion v = javaSdkType.getVersion(sdk);
      return v != null && v.isAtLeast(JavaSdkVersion.JDK_1_6) && !v.isAtLeast(JavaSdkVersion.JDK_1_9) &&
             ExternalSystemJdkUtil.isValidJdk(sdk.getHomePath());
    };

    Sdk mostRecentSdk = jdkTable.getSdksOfType(javaSdkType).stream().filter(sdkCondition).max(javaSdkType.versionComparator()).orElse(null);
    if (mostRecentSdk != null) {
      return mostRecentSdk;
    }

    Set<String> existingPaths = Arrays.stream(jdkTable.getAllJdks())
                                      .map(sdk -> sdk.getHomePath())
                                      .collect(Collectors.toCollection(() -> new THashSet<>(FileUtil.PATH_HASHING_STRATEGY)));
    for (String javaHome : javaSdkType.suggestHomePaths()) {
      if (!existingPaths.contains(FileUtil.toCanonicalPath(javaHome))) {
        Sdk jdk = javaSdkType.createJdk(ObjectUtils.notNull(javaSdkType.suggestSdkName(null, javaHome), ""), javaHome);
        if (sdkCondition.test(jdk)) {
          ApplicationManager.getApplication().runWriteAction(() -> jdkTable.addJdk(jdk));
          return jdk;
        }
      }
    }

    Project project = context.getProject() != null ? context.getProject() : ProjectManager.getInstance().getDefaultProject();
    Pair<String, Sdk> sdkPair = ExternalSystemJdkUtil.getAvailableJdk(project);
    if (!ExternalSystemJdkUtil.USE_INTERNAL_JAVA.equals(sdkPair.first)) {
      return sdkPair.second;
    }

    return null;
  }

  @Override
  protected void doPrepare(@NotNull WizardContext context) {
    String pathToUse = getFileToImport();
    VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByPath(pathToUse);
    if (file != null && !file.isDirectory() && file.getParent() != null) {
      pathToUse = file.getParent().getPath();
    }

    final ImportFromGradleControl importFromGradleControl = getControl(context.getProject());
    importFromGradleControl.setLinkedProjectPath(pathToUse);
  }

  @Override
  protected ExternalProjectRefreshCallback createFinalImportCallback(@NotNull final Project project,
                                                                     @NotNull ExternalProjectSettings projectSettings) {
    return new ExternalProjectRefreshCallback() {
      @Override
      public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
        if (externalProject == null) return;
        Runnable selectDataTask = () -> {
          ExternalProjectDataSelectorDialog dialog = new ExternalProjectDataSelectorDialog(
            project, new InternalExternalProjectInfo(
            GradleConstants.SYSTEM_ID, projectSettings.getExternalProjectPath(), externalProject));
          if (dialog.hasMultipleDataToSelect()) {
            dialog.showAndGet();
          }
          else {
            Disposer.dispose(dialog.getDisposable());
          }
        };

        Runnable importTask = () -> ServiceManager.getService(ProjectDataManager.class).importData(externalProject, project, false);

        boolean showSelectiveImportDialog = GradleSettings.getInstance(project).showSelectiveImportDialogOnInitialImport();
        if (showSelectiveImportDialog && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
          ApplicationManager.getApplication().invokeLater(() -> {
            selectDataTask.run();
            ApplicationManager.getApplication().executeOnPooledThread(importTask);
          });
        }
        else {
          importTask.run();
        }
      }
    };
  }

  @Override
  protected void beforeCommit(@NotNull DataNode<ProjectData> dataNode, @NotNull Project project) {
    if (project.getUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT) == Boolean.TRUE &&
        GradleSettings.getInstance(project).getLinkedProjectsSettings().isEmpty()) {
      ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true);
    }
    DataNode<JavaProjectData> javaProjectNode = ExternalSystemApiUtil.find(dataNode, JavaProjectData.KEY);
    if (javaProjectNode == null) {
      return;
    }

    final LanguageLevel externalLanguageLevel = javaProjectNode.getData().getLanguageLevel();
    final LanguageLevelProjectExtension languageLevelExtension = LanguageLevelProjectExtension.getInstance(project);
    if (externalLanguageLevel != languageLevelExtension.getLanguageLevel()) {
      languageLevelExtension.setLanguageLevel(externalLanguageLevel);
    }
  }

  @Override
  protected void applyExtraSettings(@NotNull WizardContext context) {
    DataNode<ProjectData> node = getExternalProjectNode();
    if (node == null) {
      return;
    }

    DataNode<JavaProjectData> javaProjectNode = ExternalSystemApiUtil.find(node, JavaProjectData.KEY);
    if (javaProjectNode != null) {
      JavaProjectData data = javaProjectNode.getData();
      context.setCompilerOutputDirectory(data.getCompileOutputPath());
      JavaSdkVersion version = data.getJdkVersion();
      Sdk jdk = JavaSdkVersionUtil.findJdkByVersion(version);
      if (jdk != null) {
        context.setProjectJdk(jdk);
      }
    }
  }

  @NotNull
  @Override
  protected File getExternalProjectConfigToUse(@NotNull File file) {
    return file.isDirectory() ? file : file.getParentFile();
  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdk) {
    return sdk == JavaSdk.getInstance();
  }

  @Nullable
  @Override
  public Project createProject(String name, String path) {
    return ExternalProjectsManagerImpl.setupCreatedProject(super.createProject(name, path));
  }

  private static GradleProjectImportBuilder ourInstance = null;

  @ApiStatus.Experimental
  static GradleProjectImportBuilder getInstance() {
    if (ourInstance == null) {
      ourInstance = new GradleProjectImportBuilder();
    }
    return ourInstance;
  }
}