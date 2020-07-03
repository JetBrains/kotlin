// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project.wizard;

import com.intellij.application.options.CodeStyle;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.EditorHelper;
import com.intellij.ide.util.projectWizard.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ModuleSdkData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.DependentSdkType;
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
import com.intellij.util.ObjectUtils;
import com.intellij.util.io.PathKt;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.frameworkSupport.KotlinBuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionUtil;
import org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleJvmResolutionUtil;
import org.jetbrains.plugins.gradle.util.GradleJvmValidationUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode.MODAL_SYNC;
import static com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.setupCreatedProject;

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

  @Nullable
  private ProjectData myParentProject;
  private boolean myInheritGroupId;
  private boolean myInheritVersion;
  private ProjectId myProjectId;
  private Path rootProjectPath;
  private boolean myUseKotlinDSL;
  private boolean isCreatingNewProject;
  private boolean isCreatingNewLinkedProject;

  public AbstractGradleModuleBuilder() {
    super(GradleConstants.SYSTEM_ID, new GradleProjectSettings());
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel)
    throws InvalidDataException, ConfigurationException {
    LOG.assertTrue(getName() != null);
    final String moduleFilePath = getModuleFilePath();
    LOG.assertTrue(moduleFilePath != null);

    deleteModuleFile(moduleFilePath);
    String moduleTypeId = getModuleType().getId();
    Module module = moduleModel.newModule(moduleFilePath, moduleTypeId);
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

    Project project = modifiableRootModel.getProject();
    Module module = modifiableRootModel.getModule();
    if (myParentProject != null) {
      rootProjectPath = Paths.get(myParentProject.getLinkedExternalProjectPath());
    }
    else {
      rootProjectPath = isCreatingNewProject ? Paths.get(Objects.requireNonNull(project.getBasePath())) : modelContentRootDir.toNioPath();
    }

    final VirtualFile gradleBuildFile = setupGradleBuildFile(modelContentRootDir);
    setupGradleSettingsFile(
      rootProjectPath, modelContentRootDir, project.getName(),
      myProjectId == null ? module.getName() : myProjectId.getArtifactId(),
      // TODO: replace with isCreatingNewLinkedProject when GradleModuleBuilder will be removed
      isCreatingNewProject || myParentProject == null,
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

    VirtualFile buildScriptFile = createAndConfigureBuildScriptFile(module);

    FileDocumentManager.getInstance().saveAllDocuments();

    // it will be set later in any case, but save is called immediately after project creation, so, to ensure that it will be properly saved as external system module
    ExternalSystemModulePropertyManager modulePropertyManager = ExternalSystemModulePropertyManager.getInstance(module);
    modulePropertyManager.setExternalId(GradleConstants.SYSTEM_ID);
    // set linked project path to be able to map the module with the module data obtained from the import
    modulePropertyManager.setRootProjectPath(PathKt.getSystemIndependentPath(rootProjectPath));
    modulePropertyManager.setLinkedProjectPath(PathKt.getSystemIndependentPath(rootProjectPath));

    Project project = module.getProject();

    GradleSettings settings = GradleSettings.getInstance(project);
    GradleProjectSettings projectSettings = getExternalProjectSettings();
    // TODO: replace with isCreatingNewLinkedProject when GradleModuleBuilder will be removed
    if (myParentProject == null) {
      GradleProjectImportUtil.setupGradleSettings(settings);
      GradleProjectImportUtil.setupGradleProjectSettings(projectSettings, rootProjectPath);
    }
    GradleVersion gradleVersion = suggestGradleVersion(project);
    if (isCreatingNewLinkedProject) {
      GradleJvmResolutionUtil.setupGradleJvm(project, projectSettings, gradleVersion);
      GradleJvmValidationUtil.validateJavaHome(project, rootProjectPath, gradleVersion);
    }
    // TODO: replace with isCreatingNewLinkedProject when GradleModuleBuilder will be removed
    if (myParentProject == null) {
      settings.linkProject(projectSettings);
    }
    if (isCreatingNewProject) {
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
      // Needed to ignore postponed project refresh
      project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
    }

    // execute when current dialog is closed
    ApplicationManager.getApplication().invokeLater(() -> {
      if (isCreatingNewProject) {
        // update external projects data to be able to add child modules before the initial import finish
        loadPreviewProject(project);
      }
      openBuildScriptFile(project, buildScriptFile);
      if (isCreatingNewLinkedProject) {
        createWrapper(project, gradleVersion, () -> {
          reloadProject(project);
        });
      }
      else {
        reloadProject(project);
      }
    }, ModalityState.NON_MODAL, project.getDisposed());
  }

  private void loadPreviewProject(@NotNull Project project) {
    ImportSpecBuilder previewSpec = new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID);
    previewSpec.usePreviewMode();
    previewSpec.use(MODAL_SYNC);
    previewSpec.callback(new ConfigureGradleModuleCallback(previewSpec));
    ExternalSystemUtil.refreshProject(PathKt.getSystemIndependentPath(rootProjectPath), previewSpec);
  }

  private void reloadProject(@NotNull Project project) {
    ImportSpecBuilder importSpec = new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID);
    importSpec.createDirectoriesForEmptyContentRoots();
    importSpec.callback(new ConfigureGradleModuleCallback(importSpec));
    ExternalSystemUtil.refreshProject(PathKt.getSystemIndependentPath(rootProjectPath), importSpec);
  }

  private void createWrapper(@NotNull Project project, @NotNull GradleVersion gradleVersion, @NotNull Runnable callback) {
    GradleExecutionUtil.ensureInstalledWrapper(project, rootProjectPath, gradleVersion, callback);
  }

  private static @NotNull GradleVersion suggestGradleVersion(@NotNull Project project) {
    GradleVersion gradleVersion = GradleJvmResolutionUtil.suggestGradleVersion(project);
    return gradleVersion == null ? GradleVersion.current() : gradleVersion;
  }

  @Nullable
  private static VirtualFile createAndConfigureBuildScriptFile(@NotNull Module module) {
    final BuildScriptDataBuilder buildScriptDataBuilder = getBuildScriptData(module);
    if (buildScriptDataBuilder == null) return null;
    try {
      VirtualFile buildScriptFile = buildScriptDataBuilder.getBuildScriptFile();
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
      return buildScriptFile;
    }
    catch (IOException e) {
      LOG.warn("Unexpected exception on applying frameworks templates", e);
    }
    return null;
  }

  private static void openBuildScriptFile(@NotNull Project project, VirtualFile buildScriptFile) {
    if (buildScriptFile == null) return;
    PsiManager psiManager = PsiManager.getInstance(project);
    PsiFile psiFile = psiManager.findFile(buildScriptFile);
    if (psiFile == null) return;
    EditorHelper.openInEditor(psiFile);
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
    return sdk instanceof JavaSdkType && !(sdk instanceof DependentSdkType);
  }

  @Override
  public String getParentGroup() {
    return JavaModuleType.JAVA_GROUP;
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
    }
    else {
      scriptName = GradleConstants.DEFAULT_SCRIPT_NAME;
    }
    VirtualFile file;
    try {
      file = getOrCreateExternalProjectConfigFile(modelContentRootDir.toNioPath(), scriptName, true);
    }
    catch (IOException e) {
      LOG.error(e);
      throw new ConfigurationException(e.getMessage());
    }

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
  public static VirtualFile setupGradleSettingsFile(@NotNull Path rootProjectPath,
                                                    @NotNull VirtualFile modelContentRootDir,
                                                    String projectName,
                                                    String moduleName,
                                                    boolean renderNewFile,
                                                    boolean useKotlinDSL) throws ConfigurationException {
    if (!renderNewFile) {
      Path settingsFile = rootProjectPath.resolve(GradleConstants.SETTINGS_FILE_NAME);
      Path kotlinKtsSettingsFile = rootProjectPath.resolve(GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME);
      useKotlinDSL = !Files.exists(settingsFile) && (Files.exists(kotlinKtsSettingsFile) || useKotlinDSL);
    }
    String scriptName = useKotlinDSL ? GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME : GradleConstants.SETTINGS_FILE_NAME;
    VirtualFile file;
    try {
      file = getOrCreateExternalProjectConfigFile(rootProjectPath, scriptName, renderNewFile);
    }
    catch (IOException e) {
      LOG.error(e);
      throw new ConfigurationException(e.getMessage());
    }

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

  private static @NotNull VirtualFile getOrCreateExternalProjectConfigFile(@NotNull Path parent,
                                                                           @NotNull String fileName,
                                                                           boolean deleteExistingFile)
    throws ConfigurationException, IOException {
    Path file = parent.resolve(fileName);
    if (deleteExistingFile) {
      Files.deleteIfExists(file);
    }

    Files.createDirectories(file.getParent());
    try {
      Files.createFile(file);
    }
    catch (FileAlreadyExistsException ignore) {
    }

    VirtualFile virtualFile = VfsUtil.findFile(file, true);
    if (virtualFile == null) {
      throw new ConfigurationException(String.format("Can't create configuration file '%s'", file));
    }
    if (virtualFile.isDirectory()) {
      throw new ConfigurationException(String.format("Configuration file is a directory '%s'", file));
    }
    VfsUtil.markDirtyAndRefresh(false, false, false, virtualFile);
    return virtualFile;
  }

  public void setParentProject(@Nullable ProjectData parentProject) {
    myParentProject = parentProject;
    isCreatingNewLinkedProject = myParentProject == null;
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

  protected boolean isCreatingNewProject() {
    return isCreatingNewProject;
  }

  protected void setCreatingNewProject(boolean creatingNewProject) {
    isCreatingNewProject = creatingNewProject;
  }

  @Override
  public void cleanup() {
    myJdk = null;
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

  private class ConfigureGradleModuleCallback implements ExternalProjectRefreshCallback {

    private final @Nullable String externalConfigPath;
    private final @Nullable String sdkName;

    private final @NotNull ImportSpecBuilder.DefaultProjectRefreshCallback defaultCallback;

    ConfigureGradleModuleCallback(@NotNull ImportSpecBuilder importSpecBuilder) {
      this.defaultCallback = new ImportSpecBuilder.DefaultProjectRefreshCallback(importSpecBuilder.build());
      this.sdkName = ObjectUtils.doIfNotNull(myJdk, it -> it.getName());
      this.externalConfigPath = FileUtil.toCanonicalPath(getContentEntryPath());
    }

    @Override
    public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
      if (externalProject != null) {
        configureModulesSdk(externalProject);
      }
      defaultCallback.onSuccess(externalProject);
    }

    private void configureModulesSdk(@NotNull DataNode<ProjectData> projectNode) {
      DataNode<ModuleData> moduleNode = ExternalSystemApiUtil.find(projectNode, ProjectKeys.MODULE, this::isTargetModule);
      if (moduleNode == null) return;
      configureModuleSdk(moduleNode);
      Collection<DataNode<GradleSourceSetData>> sourceSetsNodes = ExternalSystemApiUtil.getChildren(moduleNode, GradleSourceSetData.KEY);
      for (DataNode<GradleSourceSetData> sourceSetsNode : sourceSetsNodes) {
        configureModuleSdk(sourceSetsNode);
      }
    }

    private void configureModuleSdk(@NotNull DataNode<? extends ModuleData> moduleNode) {
      DataNode<ModuleSdkData> moduleSdkNode = ExternalSystemApiUtil.find(moduleNode, ModuleSdkData.KEY);
      if (moduleSdkNode == null) return;
      moduleSdkNode.getData().setSdkName(sdkName);
    }

    private boolean isTargetModule(@NotNull DataNode<ModuleData> moduleNode) {
      ModuleData moduleData = moduleNode.getData();
      String linkedExternalProjectPath = moduleData.getLinkedExternalProjectPath();
      return linkedExternalProjectPath.equals(externalConfigPath);
    }
  }
}
