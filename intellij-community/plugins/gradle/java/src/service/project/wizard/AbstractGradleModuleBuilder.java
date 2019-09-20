// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.EditorHelper;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalStateComponent;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.frameworkSupport.KotlinBuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.ide.util.newProjectWizard.AbstractProjectWizard.getNewProjectJdk;
import static com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.setupCreatedProject;
import static org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil.setupGradleSettings;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractGradleModuleBuilder extends AbstractExternalModuleBuilder<GradleProjectSettings> {

  private static final Logger LOG = Logger.getInstance(AbstractGradleModuleBuilder.class);

  private static final String TEMPLATE_GRADLE_SETTINGS = "Gradle Settings.gradle";
  private static final String TEMPLATE_GRADLE_SETTINGS_MERGE = "Gradle Settings merge.gradle";
  private static final String TEMPLATE_GRADLE_BUILD_WITH_WRAPPER = "Gradle Build Script with wrapper.gradle";
  private static final String DEFAULT_TEMPLATE_GRADLE_BUILD = "Gradle Build Script.gradle";
  private static final String KOTLIN_DSL_TEMPLATE_GRADLE_BUILD = "Gradle Kotlin DSL Build Script.gradle";
  private static final String KOTLIN_DSL_TEMPLATE_GRADLE_BUILD_WITH_WRAPPER = "Gradle Kotlin DSL Build Script with wrapper.gradle";
  private static final String KOTLIN_DSL_TEMPLATE_GRADLE_SETTINGS = "Gradle Kotlin DSL Settings.gradle";
  private static final String KOTLIN_DSL_TEMPLATE_GRADLE_SETTINGS_MERGE = "Gradle Kotlin DSL Settings merge.gradle";

  private static final String TEMPLATE_ATTRIBUTE_PROJECT_NAME = "PROJECT_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_PATH = "MODULE_PATH";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR = "MODULE_FLAT_DIR";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_GROUP = "MODULE_GROUP";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_VERSION = "MODULE_VERSION";
  private static final String TEMPLATE_ATTRIBUTE_GRADLE_VERSION = "GRADLE_VERSION";
  private static final Key<BuildScriptDataBuilder> BUILD_SCRIPT_DATA =
    Key.create("gradle.module.buildScriptData");

  private WizardContext myWizardContext;

  @Nullable
  private ProjectData myParentProject;
  private boolean myInheritGroupId;
  private boolean myInheritVersion;
  private ProjectId myProjectId;
  private String rootProjectPath;
  private boolean myUseKotlinDSL;

  public AbstractGradleModuleBuilder() {
    super(GradleConstants.SYSTEM_ID, new GradleProjectSettings());
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel)
    throws InvalidDataException, ConfigurationException {
    LOG.assertTrue(getName() != null);
    final String originModuleFilePath = getModuleFilePath();
    LOG.assertTrue(originModuleFilePath != null);

    String moduleName = myProjectId == null ? getName() : myProjectId.getArtifactId();
    Project contextProject = myWizardContext.getProject();
    String projectFileDirectory = null;
    if (myWizardContext.isCreatingNewProject() || contextProject == null || contextProject.getBasePath() == null) {
      projectFileDirectory = myWizardContext.getProjectFileDirectory();
    }
    else if (myWizardContext.getProjectStorageFormat() == StorageScheme.DEFAULT) {
      String moduleFileDirectory = getModuleFileDirectory();
      if (moduleFileDirectory != null) {
        projectFileDirectory = moduleFileDirectory;
      }
    }
    if (projectFileDirectory == null) {
      projectFileDirectory = contextProject.getBasePath();
    }
    if (myWizardContext.getProjectStorageFormat() == StorageScheme.DIRECTORY_BASED) {
      projectFileDirectory += "/.idea/modules";
    }
    String moduleFilePath = projectFileDirectory + "/" + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
    deleteModuleFile(moduleFilePath);
    final ModuleType moduleType = getModuleType();
    final Module module = moduleModel.newModule(moduleFilePath, moduleType.getId());
    setupModule(module);
    return module;
  }

  @Override
  public void setupRootModel(@NotNull final ModifiableRootModel modifiableRootModel) throws ConfigurationException {
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
    // todo this should be moved to generic ModuleBuilder
    if (myJdk != null) {
      modifiableRootModel.setSdk(myJdk);
    }
    else {
      modifiableRootModel.inheritSdk();
    }

    final Project project = modifiableRootModel.getProject();
    if (myParentProject != null) {
      rootProjectPath = myParentProject.getLinkedExternalProjectPath();
    }
    else {
      rootProjectPath =
        FileUtil.toCanonicalPath(myWizardContext.isCreatingNewProject() ? project.getBasePath() : modelContentRootDir.getPath());
    }
    assert rootProjectPath != null;

    final VirtualFile gradleBuildFile = setupGradleBuildFile(modelContentRootDir);
    setupGradleSettingsFile(
      rootProjectPath, modelContentRootDir, modifiableRootModel.getProject().getName(),
      myProjectId == null ? modifiableRootModel.getModule().getName() : myProjectId.getArtifactId(),
      myWizardContext.isCreatingNewProject() || myParentProject == null,
      myUseKotlinDSL
    );

    BuildScriptDataBuilder builder;
    if (myUseKotlinDSL) {
      GradleProjectSettings gradleProjectSettings = getExternalProjectSettings();
      GradleVersion version = gradleProjectSettings.resolveGradleVersion();
      builder = new KotlinBuildScriptDataBuilder(gradleBuildFile, version);
    }
    else {
      builder = new BuildScriptDataBuilder(gradleBuildFile);
    }
    modifiableRootModel.getModule().putUserData(BUILD_SCRIPT_DATA, builder);
  }

  @Override
  protected void setupModule(Module module) throws ConfigurationException {
    super.setupModule(module);
    assert rootProjectPath != null;

    VirtualFile buildScriptFile = null;
    final BuildScriptDataBuilder buildScriptDataBuilder = getBuildScriptData(module);
    try {
      if (buildScriptDataBuilder != null) {
        buildScriptFile = buildScriptDataBuilder.getBuildScriptFile();
        String lineSeparator = lineSeparator(buildScriptFile);
        String imports = StringUtil.convertLineSeparators(buildScriptDataBuilder.buildImports(), lineSeparator);
        String configurationPart = StringUtil.convertLineSeparators(buildScriptDataBuilder.buildConfigurationPart(), lineSeparator);
        String existingText = StringUtil.trimTrailing(VfsUtilCore.loadText(buildScriptFile));
        String content = (!imports.isEmpty() ? imports + lineSeparator : "") +
                         (!configurationPart.isEmpty() ? configurationPart + lineSeparator : "") +
                         (!existingText.isEmpty() ? existingText + lineSeparator : "") +
                         lineSeparator +
                         StringUtil.convertLineSeparators(buildScriptDataBuilder.buildMainPart(), lineSeparator);
        VfsUtil.saveText(buildScriptFile, content);
      }
    }
    catch (IOException e) {
      LOG.warn("Unexpected exception on applying frameworks templates", e);
    }

    // it will be set later in any case, but save is called immediately after project creation, so, to ensure that it will be properly saved as external system module
    ExternalSystemModulePropertyManager modulePropertyManager = ExternalSystemModulePropertyManager.getInstance(module);
    modulePropertyManager.setExternalId(GradleConstants.SYSTEM_ID);
    // set linked project path to be able to map the module with the module data obtained from the import
    ExternalStateComponent moduleState = modulePropertyManager.getState();
    moduleState.setRootProjectPath(rootProjectPath);
    moduleState.setLinkedProjectPath(rootProjectPath);

    final Project project = module.getProject();
    final Sdk projectSdk = getNewProjectJdk(myWizardContext);
    final GradleProjectSettings gradleProjectSettings = getExternalProjectSettings();
    if (myWizardContext.isCreatingNewProject()) {
      setupGradleSettings(gradleProjectSettings, rootProjectPath, project, projectSdk);
      AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
      //noinspection unchecked
      settings.linkProject(gradleProjectSettings);
      // update external projects data to be able to add child modules before the initial import finish
      ProjectData projectData = new ProjectData(GradleConstants.SYSTEM_ID, project.getName(), myWizardContext.getProjectFileDirectory(),
                                                gradleProjectSettings.getExternalProjectPath());
      DataNode<ProjectData> projectDataNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);
      ExternalProjectsManagerImpl.getInstance(project).updateExternalProjectData(
        new InternalExternalProjectInfo(GradleConstants.SYSTEM_ID, gradleProjectSettings.getExternalProjectPath(), projectDataNode));
    }
    else {
      FileDocumentManager.getInstance().saveAllDocuments();
      final VirtualFile finalBuildScriptFile = buildScriptFile;
      Runnable runnable = () -> {
        if (myParentProject == null) {
          setupGradleSettings(gradleProjectSettings, rootProjectPath, project, projectSdk);
          AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
          //noinspection unchecked
          settings.linkProject(gradleProjectSettings);
        }

        ImportSpec importSpec = new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
          .createDirectoriesForEmptyContentRoots()
          .build();
        ExternalSystemUtil.refreshProject(rootProjectPath, importSpec);

        final PsiFile psiFile;
        if (finalBuildScriptFile != null) {
          psiFile = PsiManager.getInstance(project).findFile(finalBuildScriptFile);
          if (psiFile != null) {
            EditorHelper.openInEditor(psiFile);
          }
        }
      };

      // execute when current dialog is closed
      Application application = ApplicationManager.getApplication();
      application.invokeLater(runnable, ModalityState.NON_MODAL, project.getDisposed());
    }
  }

  protected void setWizardContext(@NotNull WizardContext wizardContext) {
    myWizardContext = wizardContext;
  }

  @Override
  public abstract ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider);

  @Nullable
  @Override
  public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
    final GradleFrameworksWizardStep step = new GradleFrameworksWizardStep(context, this);
    Disposer.register(parentDisposable, step);
    return step;
  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdk) {
    return sdk instanceof JavaSdkType;
  }

  @Override
  public String getParentGroup() {
    return JavaModuleType.BUILD_TOOLS_GROUP;
  }

  @Override
  public int getWeight() {
    return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT;
  }

  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }

  @NotNull
  private VirtualFile setupGradleBuildFile(@NotNull VirtualFile modelContentRootDir)
    throws ConfigurationException {
    String scriptName;
    if (myUseKotlinDSL) {
      scriptName = GradleConstants.KOTLIN_DSL_SCRIPT_NAME;
    } else {
      scriptName = GradleConstants.DEFAULT_SCRIPT_NAME;
    }
    final VirtualFile file = getOrCreateExternalProjectConfigFile(modelContentRootDir.getPath(), scriptName, true);

    final String templateName;
    if (myUseKotlinDSL) {
      templateName = getExternalProjectSettings().getDistributionType() == DistributionType.WRAPPED
                     ? KOTLIN_DSL_TEMPLATE_GRADLE_BUILD_WITH_WRAPPER
                     : KOTLIN_DSL_TEMPLATE_GRADLE_BUILD;
    }
    else {
      templateName = getExternalProjectSettings().getDistributionType() == DistributionType.WRAPPED
                     ? TEMPLATE_GRADLE_BUILD_WITH_WRAPPER
                     : DEFAULT_TEMPLATE_GRADLE_BUILD;
    }

    Map<String, String> attributes = new HashMap<>();
    if (myProjectId != null) {
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_VERSION, myProjectId.getVersion());
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_GROUP, myProjectId.getGroupId());
      attributes.put(TEMPLATE_ATTRIBUTE_GRADLE_VERSION, GradleVersion.current().getVersion());
    }
    saveFile(file, templateName, attributes);
    return file;
  }

  @NotNull
  public static VirtualFile setupGradleSettingsFile(@NotNull String rootProjectPath,
                                                    @NotNull VirtualFile modelContentRootDir,
                                                    String projectName,
                                                    String moduleName,
                                                    boolean renderNewFile,
                                                    boolean useKotlinDSL) throws ConfigurationException {
    if (!renderNewFile) {
      File settingsFile = new File(rootProjectPath, GradleConstants.SETTINGS_FILE_NAME);
      File kotlinKtsSettingsFile = new File(rootProjectPath, GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME);
      useKotlinDSL = !settingsFile.exists() && (kotlinKtsSettingsFile.exists() || useKotlinDSL);
    }
    String scriptName = useKotlinDSL ? GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME : GradleConstants.SETTINGS_FILE_NAME;
    final VirtualFile file = getOrCreateExternalProjectConfigFile(rootProjectPath, scriptName, renderNewFile);

    if (renderNewFile) {
      String templateName = useKotlinDSL ? KOTLIN_DSL_TEMPLATE_GRADLE_SETTINGS : TEMPLATE_GRADLE_SETTINGS;
      final String moduleDirName = VfsUtilCore.getRelativePath(modelContentRootDir, file.getParent(), '/');

      Map<String, String> attributes = new HashMap<>();
      attributes.put(TEMPLATE_ATTRIBUTE_PROJECT_NAME, projectName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, moduleDirName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);
      saveFile(file, templateName, attributes);
    }
    else {
      String templateName = useKotlinDSL ? KOTLIN_DSL_TEMPLATE_GRADLE_SETTINGS_MERGE : TEMPLATE_GRADLE_SETTINGS_MERGE;
      char separatorChar = file.getParent() == null || !VfsUtilCore.isAncestor(file.getParent(), modelContentRootDir, true) ? '/' : ':';
      String modulePath = VfsUtilCore.findRelativePath(file, modelContentRootDir, separatorChar);

      Map<String, String> attributes = new HashMap<>();
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);
      // check for flat structure
      final String flatStructureModulePath =
        modulePath != null && StringUtil.startsWith(modulePath, "../") ? StringUtil.trimStart(modulePath, "../") : null;
      if (StringUtil.equals(flatStructureModulePath, modelContentRootDir.getName())) {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR, "true");
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, flatStructureModulePath);
      }
      else {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, modulePath);
      }

      appendToFile(file, templateName, attributes);
    }
    return file;
  }

  private static void saveFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      appendToFile(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on applying template %s config", GradleConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't apply %s template config text", GradleConstants.SYSTEM_ID.getReadableName())
      );
    }
  }

  private static void appendToFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      appendToFile(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on appending template %s config", GradleConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't append %s template config text", GradleConstants.SYSTEM_ID.getReadableName())
      );
    }
  }

  @NotNull
  private static VirtualFile getOrCreateExternalProjectConfigFile(@NotNull String parent,
                                                                  @NotNull String fileName,
                                                                  boolean deleteExistingFile)
    throws ConfigurationException {
    File file = new File(parent, fileName);
    if (deleteExistingFile) FileUtilRt.delete(file);
    FileUtilRt.createIfNotExists(file);
    VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
    if (virtualFile == null) {
      throw new ConfigurationException(String.format("Can't create configuration file '%s'", file.getPath()));
    }
    if (virtualFile.isDirectory()) {
      throw new ConfigurationException(String.format("Configuration file is a directory '%s'", file.getPath()));
    }
    VfsUtil.markDirtyAndRefresh(false, false, false, virtualFile);
    return virtualFile;
  }

  public void setParentProject(@Nullable ProjectData parentProject) {
    myParentProject = parentProject;
  }

  public boolean isInheritGroupId() {
    return myInheritGroupId;
  }

  public void setInheritGroupId(boolean inheritGroupId) {
    myInheritGroupId = inheritGroupId;
  }

  public boolean isInheritVersion() {
    return myInheritVersion;
  }

  public void setInheritVersion(boolean inheritVersion) {
    myInheritVersion = inheritVersion;
  }

  public ProjectId getProjectId() {
    return myProjectId;
  }

  public void setProjectId(@NotNull ProjectId projectId) {
    myProjectId = projectId;
  }

  @Nullable
  @Override
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    if (settingsStep instanceof ProjectSettingsStep) {
      final ProjectSettingsStep projectSettingsStep = (ProjectSettingsStep)settingsStep;
      if (myProjectId != null) {
        final ModuleNameLocationSettings nameLocationSettings = settingsStep.getModuleNameLocationSettings();
        String artifactId = myProjectId.getArtifactId();
        if (nameLocationSettings != null && artifactId != null) {
          nameLocationSettings.setModuleName(artifactId);
        }
      }
      projectSettingsStep.bindModuleSettings();
    }
    return super.modifySettingsStep(settingsStep);
  }

  public static void appendToFile(@NotNull VirtualFile file, @NotNull String text) throws IOException {
    String lineSeparator = lineSeparator(file);
    final String existingText = StringUtil.trimTrailing(VfsUtilCore.loadText(file));
    String content = (StringUtil.isNotEmpty(existingText) ? existingText + lineSeparator : "") +
                     StringUtil.convertLineSeparators(text, lineSeparator);
    VfsUtil.saveText(file, content);
  }

  @NotNull
  private static String lineSeparator(@NotNull VirtualFile file) {
    String lineSeparator = LoadTextUtil.detectLineSeparator(file, true);
    if (lineSeparator == null) {
      lineSeparator = CodeStyle.getDefaultSettings().getLineSeparator();
    }
    return lineSeparator;
  }

  @Nullable
  public static BuildScriptDataBuilder getBuildScriptData(@Nullable Module module) {
    return module == null ? null : module.getUserData(BUILD_SCRIPT_DATA);
  }

  @Nullable
  @Override
  public Project createProject(String name, String path) {
    return setupCreatedProject(super.createProject(name, path));
  }

  public void setUseKotlinDsl(boolean useKotlinDSL) {
    myUseKotlinDSL = useKotlinDSL;
  }
}
