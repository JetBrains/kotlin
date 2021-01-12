package com.intellij.openapi.externalSystem.service.remote;

import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines common interface for resolving gradle project, i.e. building object-level representation of {@code 'build.gradle'}.
 * 
 * @author Denis Zhdanov
 */
public class RemoteExternalSystemProjectResolverImpl<S extends ExternalSystemExecutionSettings>
  extends AbstractRemoteExternalSystemService<S> implements RemoteExternalSystemProjectResolver<S>
{

  private final ExternalSystemProjectResolver<S> myDelegate;

  public RemoteExternalSystemProjectResolverImpl(@NotNull ExternalSystemProjectResolver<S> delegate) {
    myDelegate = delegate;
  }

  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(@NotNull ExternalSystemTaskId id,
                                                  @NotNull String projectPath,
                                                  boolean isPreviewMode,
                                                  @Nullable S settings,
                                                  @Nullable ProjectResolverPolicy resolverPolicy)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    return execute(id, () ->
      myDelegate.resolveProjectInfo(id, projectPath, isPreviewMode, settings, resolverPolicy, getNotificationListener()));
  }

  @Override
  public boolean cancelTask(@NotNull final ExternalSystemTaskId id)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    return myDelegate.cancelTask(id, getNotificationListener());
  }
}
