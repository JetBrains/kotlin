/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.service.notification.callback;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.externalSystem.ExternalSystemConfigurableAware;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * @author Vladislav.Soroka
 */
public class OpenExternalSystemSettingsCallback extends NotificationListener.Adapter {

  public final static String ID = "#open_external_system_settings";
  private final Project myProject;
  @NotNull private final ProjectSystemId mySystemId;
  @Nullable private final String myLinkedProjectPath;

  public OpenExternalSystemSettingsCallback(Project project, @NotNull ProjectSystemId systemId) {
    this(project, systemId, null);
  }

  public OpenExternalSystemSettingsCallback(Project project, @NotNull ProjectSystemId systemId, @Nullable String linkedProjectPath) {
    myProject = project;
    mySystemId = systemId;
    myLinkedProjectPath = linkedProjectPath;
  }

  @Override
  protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {

    ExternalSystemManager<?, ?, ?, ?, ?> manager;
    if (myLinkedProjectPath == null ||
        !((manager = ExternalSystemApiUtil.getManager(mySystemId)) instanceof ExternalSystemConfigurableAware)) {
      ShowSettingsUtil.getInstance().showSettingsDialog(myProject, mySystemId.getReadableName());
      return;
    }
    final Configurable configurable = ((ExternalSystemConfigurableAware)manager).getConfigurable(myProject);
    if(configurable instanceof AbstractExternalSystemConfigurable) {
      ShowSettingsUtil.getInstance().editConfigurable(myProject, configurable,
                                                      () -> ((AbstractExternalSystemConfigurable)configurable).selectProject(myLinkedProjectPath));
    }

  }
}
