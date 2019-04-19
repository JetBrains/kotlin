// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.getExternalProjectPath;

/**
 * @author Vladislav.Soroka
 */
public class GradleEnvironmentTest extends GradleImportingTestCase {
  @Test
  @TargetVersions("3.5+")
  public void testGradleEnvCustomization() throws Exception {
    Map<String, String> passedEnv = Collections.singletonMap("FOO", "foo value");
    StringBuilder gradleEnv = new StringBuilder();

    importAndRunTask(passedEnv, gradleEnv);

    assertEquals(DefaultGroovyMethods.toMapString(passedEnv), gradleEnv.toString().trim());
  }

  @Test
  @TargetVersions("3.4")
  public void testGradleEnvCustomizationNotSupported() throws Exception {
    Map<String, String> passedEnv = Collections.singletonMap("FOO", "foo value");
    StringBuilder gradleEnv = new StringBuilder();
    importAndRunTask(passedEnv, gradleEnv);
    assertTrue(gradleEnv.toString().trim().startsWith(
      "The version of Gradle you are using (3.4) does not support the environment variables customization feature. " +
      "Support for this is available in Gradle 3.5 and all later versions."));
  }

  private void importAndRunTask(@NotNull Map<String, String> passedEnv, StringBuilder gradleEnv) throws IOException {
    importProject("task printEnv() {\n" +
                  "  doLast { println System.getenv().toMapString()}\n" +
                  "}");

    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    Module module = getModule("project");
    settings.setExternalProjectPath(getExternalProjectPath(module));
    settings.setTaskNames(Collections.singletonList("printEnv"));
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    settings.setScriptParameters("--quiet");
    settings.setPassParentEnvs(false);
    settings.setEnv(passedEnv);
    ExternalSystemProgressNotificationManager notificationManager =
      ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    ExternalSystemTaskNotificationListenerAdapter listener = new ExternalSystemTaskNotificationListenerAdapter() {
      @Override
      public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
        gradleEnv.append(text);
      }
    };
    notificationManager.addNotificationListener(listener);
    try {
      ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, myProject, GradleConstants.SYSTEM_ID, null,
                                 ProgressExecutionMode.NO_PROGRESS_SYNC);
    }
    finally {
      notificationManager.removeNotificationListener(listener);
    }
  }
}
