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
package org.jetbrains.plugins.gradle.execution.test.runner.events;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;
import org.jetbrains.plugins.gradle.service.project.GradleNotification;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.event.HyperlinkEvent;

/**
 * @author Vladislav.Soroka
 */
public class ConfigurationErrorEvent extends AbstractTestEvent {

  public ConfigurationErrorEvent(GradleTestsExecutionConsole executionConsole) {
    super(executionConsole);
  }

  @Override
  public void process(@NotNull final TestEventXmlView xml) throws TestEventXmlView.XmlParserException {
    final String errorTitle = xml.getEventTitle();
    final String configurationErrorMsg = xml.getEventMessage();
    final boolean openSettings = xml.isEventOpenSettings();
    final Project project = getProject();
    assert project != null;
    final String message =
      openSettings ? String.format("<br>\n%s<br><br>\n\n<a href=\"Gradle settings\">Open gradle settings</a>", configurationErrorMsg)
                   : String.format("<br>\n%s", configurationErrorMsg);
    GradleNotification.getInstance(project).showBalloon(
      errorTitle,
      message, NotificationType.WARNING, new NotificationListener() {
        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
          notification.expire();
          if ("Gradle settings".equals(event.getDescription())) {
            ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
            assert manager instanceof GradleManager;
            GradleManager gradleManager = (GradleManager)manager;
            Configurable configurable = gradleManager.getConfigurable(project);
            ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
          }
          else {
            BrowserUtil.browse(event.getDescription());
          }
        }
      }
    );
  }
}
