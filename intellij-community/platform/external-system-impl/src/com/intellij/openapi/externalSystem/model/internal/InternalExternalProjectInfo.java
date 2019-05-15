// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.model.internal;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InternalExternalProjectInfo implements ExternalProjectInfo {
  @NotNull
  private final ProjectSystemId projectSystemId;
  @NotNull
  private final String externalProjectPath;
  @Nullable
  private final DataNode<ProjectData> externalProjectStructure;

  private long lastSuccessfulImportTimestamp = -1;
  private long lastImportTimestamp = -1;

  private final String buildNumber;

  public InternalExternalProjectInfo(@NotNull ProjectSystemId projectSystemId,
                                     @NotNull String externalProjectPath,
                                     @Nullable DataNode<ProjectData> externalProjectStructure) {
    this.projectSystemId = projectSystemId;
    this.externalProjectPath = externalProjectPath;
    this.externalProjectStructure = externalProjectStructure;
    buildNumber = ApplicationInfo.getInstance().getBuild().asString();
  }

  // InternalExternalProjectInfo is quite huge, better to not use PropertyMapping
  @SuppressWarnings("unused")
  private InternalExternalProjectInfo() {
    projectSystemId = ProjectSystemId.IDE;
    externalProjectPath = "";
    externalProjectStructure = null;
    buildNumber = ApplicationManager.getApplication() == null ? "" : ApplicationInfo.getInstance().getBuild().asString();
  }

  @Override
  @NotNull
  public ProjectSystemId getProjectSystemId() {
    return projectSystemId;
  }

  @Override
  @NotNull
  public String getExternalProjectPath() {
    return externalProjectPath;
  }

  @SuppressWarnings("ConstantConditions")
  @Nullable
  public String getNullSafeExternalProjectPath() {
    return externalProjectPath;
  }

  @Override
  @Nullable
  public DataNode<ProjectData> getExternalProjectStructure() {
    return externalProjectStructure;
  }

  @Override
  public long getLastSuccessfulImportTimestamp() {
    return lastSuccessfulImportTimestamp;
  }

  @Override
  public long getLastImportTimestamp() {
    return lastImportTimestamp;
  }

  public void setLastSuccessfulImportTimestamp(long value) {
    lastSuccessfulImportTimestamp = value;
  }

  public void setLastImportTimestamp(long value) {
    lastImportTimestamp = value;
  }

  @Override
  public String getBuildNumber() {
    return buildNumber;
  }

  @Override
  public ExternalProjectInfo copy() {
    InternalExternalProjectInfo copy = new InternalExternalProjectInfo(
      projectSystemId,
      externalProjectPath,
      externalProjectStructure != null ? externalProjectStructure.graphCopy() : null
    );
    copy.setLastImportTimestamp(lastImportTimestamp);
    copy.setLastSuccessfulImportTimestamp(lastSuccessfulImportTimestamp);
    return copy;
  }

  @Override
  public String toString() {
    return "InternalExternalProjectInfo{" +
           "myProjectSystemId=" + projectSystemId +
           ", externalProjectPath='" + externalProjectPath + '\'' +
           ", externalProjectStructure=" + externalProjectStructure +
           ", lastSuccessfulImportTimestamp=" + lastSuccessfulImportTimestamp +
           ", lastImportTimestamp=" + lastImportTimestamp +
           '}';
  }
}
