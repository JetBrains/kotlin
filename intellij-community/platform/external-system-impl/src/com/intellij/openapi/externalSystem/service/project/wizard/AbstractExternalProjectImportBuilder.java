// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.*;
import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.externalSystem.service.ui.ExternalProjectDataSelectorDialog;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * GoF builder for external system backed projects.
 *
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalProjectImportBuilder<C extends AbstractImportFromExternalSystemControl>
  extends ProjectImportBuilder<DataNode<ProjectData>>
{

  private static final Logger LOG = Logger.getInstance(AbstractExternalProjectImportBuilder.class);

  @NotNull private final ProjectDataManager myProjectDataManager;
  @NotNull private final NotNullLazyValue<C> myControlValue;
  @NotNull private final ProjectSystemId myExternalSystemId;

  private DataNode<ProjectData> myExternalProjectNode;

  /**
   * @deprecated use {@link AbstractExternalProjectImportBuilder#AbstractExternalProjectImportBuilder(ProjectDataManager, NotNullFactory, ProjectSystemId)}
   */
  @Deprecated
  public AbstractExternalProjectImportBuilder(@NotNull com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager projectDataManager,
                                              @NotNull C control,
                                              @NotNull ProjectSystemId externalSystemId) {
    this((ProjectDataManager)projectDataManager, control, externalSystemId);
  }

  /**
   * @deprecated use {@link AbstractExternalProjectImportBuilder#AbstractExternalProjectImportBuilder(ProjectDataManager, NotNullFactory, ProjectSystemId)}
   */
  @Deprecated
  public AbstractExternalProjectImportBuilder(@NotNull ProjectDataManager projectDataManager,
                                              @NotNull C control,
                                              @NotNull ProjectSystemId externalSystemId) {
    myProjectDataManager = projectDataManager;
    myControlValue = NotNullLazyValue.createValue(() -> control);
    myExternalSystemId = externalSystemId;
  }

  public AbstractExternalProjectImportBuilder(@NotNull ProjectDataManager projectDataManager,
                                              @NotNull NotNullFactory<? extends C> controlFactory,
                                              @NotNull ProjectSystemId externalSystemId) {
    myProjectDataManager = projectDataManager;
    myControlValue = NotNullLazyValue.createValue(controlFactory);
    myExternalSystemId = externalSystemId;
  }

  @Override
  public List<DataNode<ProjectData>> getList() {
    return Collections.singletonList(myExternalProjectNode);
  }

  @Override
  public boolean isMarked(DataNode<ProjectData> element) {
    return true;
  }

  @Override
  public void setList(List<DataNode<ProjectData>> externalSystemProjects) {
  }

  @Override
  public void setOpenProjectSettingsAfter(boolean on) {
  }

  @NotNull
  public C getControl(@Nullable Project currentProject) {
    C control = getControl();
    control.setCurrentProject(currentProject);
    return control;
  }

  public void prepare(@NotNull WizardContext context) {
    if (context.getProjectJdk() == null) {
      context.setProjectJdk(resolveProjectJdk(context));
    }
    C control = getControl();
    control.setShowProjectFormatPanel(context.isCreatingNewProject());
    control.reset(context, null);
    String pathToUse = getFileToImport();
    control.setLinkedProjectPath(pathToUse);
    doPrepare(context);
  }

  @Nullable
  protected Sdk resolveProjectJdk(@NotNull WizardContext context) {
    Project project = context.getProject() != null ? context.getProject() : ProjectManager.getInstance().getDefaultProject();
    final Pair<String, Sdk> sdkPair = ExternalSystemJdkUtil.getAvailableJdk(project);
    if (!ExternalSystemJdkUtil.USE_INTERNAL_JAVA.equals(sdkPair.first)) {
      return sdkPair.second;
    }
    return null;
  }

  protected abstract void doPrepare(@NotNull WizardContext context);

  @Override
  public List<Module> commit(final Project project,
                             final ModifiableModuleModel model,
                             final ModulesProvider modulesProvider,
                             final ModifiableArtifactModel artifactModel)
  {
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
    final DataNode<ProjectData> externalProjectNode = getExternalProjectNode();
    if (externalProjectNode != null) {
      beforeCommit(externalProjectNode, project);
    }

    final boolean isFromUI = model != null;

    final List<Module> modules = ContainerUtil.newSmartList();
    final Map<ModifiableRootModel, Module> moduleMap = ContainerUtil.newIdentityHashMap();
    final IdeModifiableModelsProvider modelsProvider = isFromUI ? new IdeUIModifiableModelsProvider(
      project, model, (ModulesConfigurator)modulesProvider, artifactModel) {

      @Override
      protected ModifiableRootModel doGetModifiableRootModel(Module module) {
        ModifiableRootModel modifiableRootModel = super.doGetModifiableRootModel(module);
        moduleMap.put(modifiableRootModel, module);
        return modifiableRootModel;
      }

      @Override
      public void commit() {
        super.commit();
        for (Map.Entry<ModifiableRootModel, Module> moduleEntry : moduleMap.entrySet()) {
          modules.add(moduleEntry.getValue());
        }
      }
    } : new IdeModifiableModelsProviderImpl(project){
      @NotNull
      @Override
      protected ModifiableRootModel doGetModifiableRootModel(@NotNull Module module) {
        ModifiableRootModel modifiableRootModel = super.doGetModifiableRootModel(module);
        moduleMap.put(modifiableRootModel, module);
        return modifiableRootModel;
      }

      @Override
      public void commit() {
        super.commit();
        for (Map.Entry<ModifiableRootModel, Module> moduleEntry : moduleMap.entrySet()) {
          if (!moduleEntry.getKey().isWritable()) {
            modules.add(moduleEntry.getValue());
          }
        }
      }
    };
    AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, myExternalSystemId);
    final ExternalProjectSettings projectSettings = getCurrentExternalProjectSettings();

    //noinspection unchecked
    Set<ExternalProjectSettings> projects = ContainerUtilRt.newHashSet(systemSettings.getLinkedProjectsSettings());
    // add current importing project settings to linked projects settings or replace if similar already exist
    projects.remove(projectSettings);
    projects.add(projectSettings);

    //noinspection unchecked
    systemSettings.copyFrom(getControl().getSystemSettings());
    //noinspection unchecked
    systemSettings.setLinkedProjectsSettings(projects);

    if (externalProjectNode != null) {
      if (systemSettings.showSelectiveImportDialogOnInitialImport() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
        ExternalProjectDataSelectorDialog dialog = new ExternalProjectDataSelectorDialog(
          project, new InternalExternalProjectInfo(myExternalSystemId, projectSettings.getExternalProjectPath(), externalProjectNode));
        if (dialog.hasMultipleDataToSelect()) {
          dialog.showAndGet();
        } else {
          Disposer.dispose(dialog.getDisposable());
        }
      }

      if (!project.isInitialized()) {
        StartupManager.getInstance(project).runWhenProjectIsInitialized(
          () -> finishImport(project, externalProjectNode, isFromUI, modules, modelsProvider, projectSettings));
      }
      else finishImport(project, externalProjectNode, isFromUI, modules, modelsProvider, projectSettings);
    }
    return modules;
  }

  protected void finishImport(final Project project,
                              DataNode<ProjectData> externalProjectNode,
                              boolean isFromUI,
                              final List<Module> modules,
                              IdeModifiableModelsProvider modelsProvider, final ExternalProjectSettings projectSettings) {
    myProjectDataManager.importData(externalProjectNode, project, modelsProvider, true);
    myExternalProjectNode = null;

    // resolve dependencies
    final Runnable resolveDependenciesTask = () -> ExternalSystemUtil.refreshProject(
      project, myExternalSystemId, projectSettings.getExternalProjectPath(),
      createFinalImportCallback(project, projectSettings), false, ProgressExecutionMode.IN_BACKGROUND_ASYNC, true);
    if (!isFromUI) {
      resolveDependenciesTask.run();
    }
    else {
      // execute when current dialog is closed
      ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL, () -> {
        final Module[] committedModules = ModuleManager.getInstance(project).getModules();
        if (Arrays.asList(committedModules).containsAll(modules)) {
          resolveDependenciesTask.run();
        }
      });
    }
  }

  protected ExternalProjectRefreshCallback createFinalImportCallback(@NotNull Project project,
                                                                     @NotNull ExternalProjectSettings projectSettings) {
    return new ExternalProjectRefreshCallback() {
      @Override
      public void onSuccess(@Nullable final DataNode<ProjectData> externalProject) {
        if (externalProject == null) {
          return;
        }
        ServiceManager.getService(ProjectDataManager.class).importData(externalProject, project, false);
      }
    };
  }

  @NotNull
  private ExternalProjectSettings getCurrentExternalProjectSettings() {
    ExternalProjectSettings result = getControl().getProjectSettings().clone();
    File externalProjectConfigFile = getExternalProjectConfigToUse(new File(result.getExternalProjectPath()));
    final String linkedProjectPath = FileUtil.toCanonicalPath(externalProjectConfigFile.getPath());
    assert linkedProjectPath != null;
    result.setExternalProjectPath(linkedProjectPath);
    return result;
  }

  protected abstract void beforeCommit(@NotNull DataNode<ProjectData> dataNode, @NotNull Project project);

  @Nullable
  private File getProjectFile() {
    String path = getControl().getProjectSettings().getExternalProjectPath();
    return path == null ? null : new File(path);
  }

  /**
   * Asks current builder to ensure that target external project is defined.
   *
   * @param wizardContext             current wizard context
   * @throws ConfigurationException   if external project is not defined and can't be constructed
   */
  public void ensureProjectIsDefined(@NotNull WizardContext wizardContext) throws ConfigurationException {
    final String externalSystemName = myExternalSystemId.getReadableName();
    File projectFile = getProjectFile();
    if (projectFile == null) {
      throw new ConfigurationException(ExternalSystemBundle.message("error.project.undefined"));
    }
    projectFile = getExternalProjectConfigToUse(projectFile);
    final Ref<ConfigurationException> error = new Ref<>();
    final ExternalProjectRefreshCallback callback = new ExternalProjectRefreshCallback() {
      @Override
      public void onSuccess(@Nullable DataNode<ProjectData> externalProject) {
        myExternalProjectNode = externalProject;
      }

      @Override
      public void onFailure(@NotNull String errorMessage, @Nullable String errorDetails) {
        if (!StringUtil.isEmpty(errorDetails)) {
          LOG.warn(errorDetails);
        }
        error.set(new ConfigurationException(
          ExternalSystemBundle.message("error.resolve.with.log_link", errorMessage, PathManager.getLogPath()),
          ExternalSystemBundle.message("error.resolve.generic")));
      }
    };

    final Project project = getProject(wizardContext);
    final File finalProjectFile = projectFile;
    final String externalProjectPath = FileUtil.toCanonicalPath(finalProjectFile.getAbsolutePath());
    final Ref<ConfigurationException> exRef = new Ref<>();
    executeAndRestoreDefaultProjectSettings(project, () -> {
      try {
        ExternalSystemUtil.refreshProject(
          project,
          myExternalSystemId,
          externalProjectPath,
          callback,
          true,
          ProgressExecutionMode.MODAL_SYNC
        );
      }
      catch (IllegalArgumentException e) {
        exRef.set(
          new ConfigurationException(e.getMessage(), ExternalSystemBundle.message("error.cannot.parse.project", externalSystemName)));
      }
    });
    ConfigurationException ex = exRef.get();
    if (ex != null) {
      throw ex;
    }
    if (myExternalProjectNode == null) {
      ConfigurationException exception = error.get();
      if (exception != null) {
        throw exception;
      }
    }
    else {
      applyProjectSettings(wizardContext);
    }
  }

  @SuppressWarnings("unchecked")
  private void executeAndRestoreDefaultProjectSettings(@NotNull Project project, @NotNull Runnable task) {
    AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(project, myExternalSystemId);
    Object systemStateToRestore = null;
    if (systemSettings instanceof PersistentStateComponent) {
      systemStateToRestore = ((PersistentStateComponent)systemSettings).getState();
    }
    systemSettings.copyFrom(getControl().getSystemSettings());
    Collection projectSettingsToRestore = systemSettings.getLinkedProjectsSettings();
    Set<ExternalProjectSettings> projects = ContainerUtilRt.newHashSet(systemSettings.getLinkedProjectsSettings());
    projects.add(getCurrentExternalProjectSettings());
    systemSettings.setLinkedProjectsSettings(projects);

    try {
      task.run();
    }
    finally {
      if (systemStateToRestore != null) {
        ((PersistentStateComponent)systemSettings).loadState(systemStateToRestore);
      }
      else {
        systemSettings.setLinkedProjectsSettings(projectSettingsToRestore);
      }
    }
  }

  /**
   * Allows to adjust external project config file to use on the basis of the given value.
   * <p/>
   * Example: a user might choose a directory which contains target config file and particular implementation expands
   * that to a particular file under the directory.
   *
   * @param file  base external project config file
   * @return      external project config file to use
   */
  @NotNull
  protected abstract File getExternalProjectConfigToUse(@NotNull File file);

  @Nullable
  public DataNode<ProjectData> getExternalProjectNode() {
    return myExternalProjectNode;
  }

  /**
   * Applies external system-specific settings like project files location etc to the given context.
   *
   * @param context  storage for the project/module settings.
   */
  public void applyProjectSettings(@NotNull WizardContext context) {
    if (myExternalProjectNode == null) {
      assert false;
      return;
    }
    context.setProjectName(myExternalProjectNode.getData().getInternalName());
    context.setProjectFileDirectory(myExternalProjectNode.getData().getIdeProjectFileDirectoryPath());
    applyExtraSettings(context);
  }

  protected abstract void applyExtraSettings(@NotNull WizardContext context);

  /**
   * Allows to get {@link Project} instance to use. Basically, there are two alternatives -
   * {@link WizardContext#getProject() project from the current wizard context} and
   * {@link ProjectManager#getDefaultProject() default project}.
   *
   * @param wizardContext   current wizard context
   * @return                {@link Project} instance to use
   */
  @NotNull
  public Project getProject(@NotNull WizardContext wizardContext) {
    Project result = wizardContext.getProject();
    if (result == null) {
      result = ProjectManager.getInstance().getDefaultProject();
    }
    return result;
  }

  @Nullable
  @Override
  public Project createProject(String name, String path) {
    Project project = super.createProject(name, path);
    if (project != null) {
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
    }
    return project;
  }

  @NotNull
  private C getControl() {
    return myControlValue.getValue();
  }

}
