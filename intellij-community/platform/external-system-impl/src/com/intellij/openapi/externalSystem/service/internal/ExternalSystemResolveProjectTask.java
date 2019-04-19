package com.intellij.openapi.externalSystem.service.internal;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskState;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManagerImpl;
import com.intellij.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe.
 *
 * @author Denis Zhdanov
 */
public class ExternalSystemResolveProjectTask extends AbstractExternalSystemTask {

  private final AtomicReference<DataNode<ProjectData>> myExternalProject = new AtomicReference<>();

  @NotNull private final String myProjectPath;
  private final boolean myIsPreviewMode;
  @Nullable private final String myVmOptions;
  @Nullable private final String myArguments;

  public ExternalSystemResolveProjectTask(@NotNull ProjectSystemId externalSystemId,
                                          @NotNull Project project,
                                          @NotNull String projectPath,
                                          boolean isPreviewMode) {
    this(externalSystemId, project, projectPath, null, null, isPreviewMode);
  }

  public ExternalSystemResolveProjectTask(@NotNull ProjectSystemId externalSystemId,
                                          @NotNull Project project,
                                          @NotNull String projectPath,
                                          @Nullable String vmOptions,
                                          @Nullable String arguments,
                                          boolean isPreviewMode) {
    super(externalSystemId, ExternalSystemTaskType.RESOLVE_PROJECT, project, projectPath);
    myProjectPath = projectPath;
    myIsPreviewMode = isPreviewMode;
    myVmOptions = vmOptions;
    myArguments = arguments;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void doExecute() throws Exception {
    ExternalSystemProgressNotificationManagerImpl progressNotificationManager =
      (ExternalSystemProgressNotificationManagerImpl)ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    ExternalSystemTaskId id = getId();

    Project ideProject;
    RemoteExternalSystemProjectResolver resolver;
    ExternalSystemExecutionSettings settings;
    try {
      progressNotificationManager.onStart(id, myProjectPath);

      final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
      ideProject = getIdeProject();
      resolver = manager.getFacade(ideProject, myProjectPath, getExternalSystemId()).getResolver();
      settings = ExternalSystemApiUtil.getExecutionSettings(ideProject, myProjectPath, getExternalSystemId());
      if (StringUtil.isNotEmpty(myVmOptions)) {
        settings.withVmOptions(ParametersListUtil.parse(myVmOptions));
      }
      if (StringUtil.isNotEmpty(myArguments)) {
        settings.withArguments(ParametersListUtil.parse(myArguments));
      }
    }
    catch (Exception e) {
      progressNotificationManager.onFailure(id, e);
      progressNotificationManager.onEnd(id);
      throw e;
    }

    try {
      DataNode<ProjectData> project = resolver.resolveProjectInfo(id, myProjectPath, myIsPreviewMode, settings);
      if (project != null) {
        myExternalProject.set(project);

        ExternalSystemManager<?, ?, ?, ?, ?> systemManager = ExternalSystemApiUtil.getManager(getExternalSystemId());
        assert systemManager != null;

        Set<String> externalModulePaths = ContainerUtil.newHashSet();
        Collection<DataNode<ModuleData>> moduleNodes = ExternalSystemApiUtil.findAll(project, ProjectKeys.MODULE);
        for (DataNode<ModuleData> node : moduleNodes) {
          externalModulePaths.add(node.getData().getLinkedExternalProjectPath());
        }
        String projectPath = project.getData().getLinkedExternalProjectPath();
        ExternalProjectSettings linkedProjectSettings =
          systemManager.getSettingsProvider().fun(ideProject).getLinkedProjectSettings(projectPath);
        if (linkedProjectSettings != null) {
          linkedProjectSettings.setModules(externalModulePaths);
        }
      }
      progressNotificationManager.onSuccess(id);
    }
    finally {
      progressNotificationManager.onEnd(id);
    }
  }

  @Override
  protected boolean doCancel() throws Exception {
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    Project ideProject = getIdeProject();
    RemoteExternalSystemProjectResolver resolver = manager.getFacade(ideProject, myProjectPath, getExternalSystemId()).getResolver();

    return resolver.cancelTask(getId());
  }

  @Override
  @NotNull
  protected String wrapProgressText(@NotNull String text) {
    return ExternalSystemBundle.message("progress.update.text", getExternalSystemId().getReadableName(), text);
  }

  @Override
  protected void setState(@NotNull ExternalSystemTaskState state) {
    super.setState(state);
    if (state.isStopped()) {
      InternalExternalProjectInfo projectInfo =
        new InternalExternalProjectInfo(getExternalSystemId(), getExternalProjectPath(), myExternalProject.getAndSet(null));
      final long currentTimeMillis = System.currentTimeMillis();
      projectInfo.setLastImportTimestamp(currentTimeMillis);
      projectInfo.setLastSuccessfulImportTimestamp(state == ExternalSystemTaskState.FAILED ? -1 : currentTimeMillis);
      ProjectDataManagerImpl.getInstance().updateExternalProjectData(getIdeProject(), projectInfo);
    }
  }
}
