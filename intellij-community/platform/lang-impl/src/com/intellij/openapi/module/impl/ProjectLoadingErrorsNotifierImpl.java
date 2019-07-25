/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.openapi.module.impl;

import com.intellij.CommonBundle;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.ConfigurationErrorDescription;
import com.intellij.openapi.module.ConfigurationErrorType;
import com.intellij.openapi.module.ProjectLoadingErrorsNotifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ProjectLoadingErrorsNotifierImpl extends ProjectLoadingErrorsNotifier {
  private final MultiMap<ConfigurationErrorType, ConfigurationErrorDescription> myErrors = new MultiMap<>();
  private final Object myLock = new Object();
  private final Project myProject;

  public ProjectLoadingErrorsNotifierImpl(Project project) {
    myProject = project;
  }

  @Override
  public void registerError(@NotNull ConfigurationErrorDescription errorDescription) {
    registerErrors(Collections.singletonList(errorDescription));
  }

  @Override
  public void registerErrors(@NotNull Collection<? extends ConfigurationErrorDescription> errorDescriptions) {
    if (myProject.isDisposed() || myProject.isDefault() || errorDescriptions.isEmpty()) return;

    boolean first;
    synchronized (myLock) {
      first = myErrors.isEmpty();
      for (ConfigurationErrorDescription description : errorDescriptions) {
        myErrors.putValue(description.getErrorType(), description);
      }
    }
    if (myProject.isInitialized()) {
      fireNotifications();
    }
    else if (first) {
      StartupManager.getInstance(myProject).registerPostStartupActivity(() -> fireNotifications());
    }
  }

  private void fireNotifications() {
    final MultiMap<ConfigurationErrorType, ConfigurationErrorDescription> descriptionsMap = new MultiMap<>();
    synchronized (myLock) {
      if (myErrors.isEmpty()) return;
      descriptionsMap.putAllValues(myErrors);
      myErrors.clear();
    }

    for (final ConfigurationErrorType type : descriptionsMap.keySet()) {
      final Collection<ConfigurationErrorDescription> descriptions = descriptionsMap.get(type);
      if (descriptions.isEmpty()) continue;

      final String invalidElements = getInvalidElementsString(type, descriptions);
      final String errorText = ProjectBundle.message("error.message.configuration.cannot.load") + " " + invalidElements + " <a href=\"\">Details...</a>";

      Notifications.Bus.notify(new Notification("Project Loading Error", "Error Loading Project", errorText, NotificationType.ERROR,
                                                new NotificationListener() {
                                                  @Override
                                                  public void hyperlinkUpdate(@NotNull Notification notification,
                                                                              @NotNull HyperlinkEvent event) {
                                                    final List<ConfigurationErrorDescription> validDescriptions =
                                                      ContainerUtil.findAll(descriptions, errorDescription -> errorDescription.isValid());
                                                    if (RemoveInvalidElementsDialog.showDialog(myProject, CommonBundle.getErrorTitle(), type,
                                                                                               invalidElements, validDescriptions)) {
                                                      notification.expire();
                                                    }
                                                  }
                                                }), myProject);
    }

  }

  private static String getInvalidElementsString(ConfigurationErrorType type, Collection<ConfigurationErrorDescription> descriptions) {
    if (descriptions.size() == 1) {
      final ConfigurationErrorDescription description = ContainerUtil.getFirstItem(descriptions);
      return type.getElementKind() + " " + description.getElementName();
    }

    return descriptions.size() + " " + StringUtil.pluralize(type.getElementKind());
  }
}
