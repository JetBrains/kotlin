// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemBeforeRunTask extends BeforeRunTask<ExternalSystemBeforeRunTask> {
  @NotNull
  private final ExternalSystemTaskExecutionSettings myTaskExecutionSettings;

  public ExternalSystemBeforeRunTask(@NotNull Key<ExternalSystemBeforeRunTask> providerId, @NotNull ProjectSystemId systemId) {
    super(providerId);
    myTaskExecutionSettings = new ExternalSystemTaskExecutionSettings();
    myTaskExecutionSettings.setExternalSystemIdString(systemId.getId());
  }

  private ExternalSystemBeforeRunTask(@NotNull ExternalSystemBeforeRunTask source) {
    super(source.myProviderId);
    myTaskExecutionSettings = source.myTaskExecutionSettings.clone();
  }

  @NotNull
  public ExternalSystemTaskExecutionSettings getTaskExecutionSettings() {
    return myTaskExecutionSettings;
  }

  @Override
  public void writeExternal(@NotNull Element element) {
    super.writeExternal(element);

    element.setAttribute("tasks", StringUtil.join(myTaskExecutionSettings.getTaskNames(), " "));
    if (myTaskExecutionSettings.getExternalProjectPath() != null) {
      element.setAttribute("externalProjectPath", myTaskExecutionSettings.getExternalProjectPath());
    }
    if (myTaskExecutionSettings.getVmOptions() != null) element.setAttribute("vmOptions", myTaskExecutionSettings.getVmOptions());
    if (myTaskExecutionSettings.getScriptParameters() != null) {
      element.setAttribute("scriptParameters", myTaskExecutionSettings.getScriptParameters());
    }
  }

  @Override
  public void readExternal(@NotNull Element element) {
    super.readExternal(element);
    myTaskExecutionSettings.setTaskNames(StringUtil.split(StringUtil.notNullize(element.getAttributeValue("tasks")), " "));
    myTaskExecutionSettings.setExternalProjectPath(element.getAttributeValue("externalProjectPath"));
    myTaskExecutionSettings.setVmOptions(element.getAttributeValue("vmOptions"));
    myTaskExecutionSettings.setScriptParameters(element.getAttributeValue("scriptParameters"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ExternalSystemBeforeRunTask)) return false;
    if (!super.equals(o)) return false;

    ExternalSystemBeforeRunTask task = (ExternalSystemBeforeRunTask)o;

    if (!myTaskExecutionSettings.equals(task.myTaskExecutionSettings)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myTaskExecutionSettings.hashCode();
    return result;
  }

  @Override
  public BeforeRunTask clone() {
    return new ExternalSystemBeforeRunTask(this);
  }
}
