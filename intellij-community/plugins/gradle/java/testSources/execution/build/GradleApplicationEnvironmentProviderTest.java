// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.build;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.concurrency.Semaphore;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx;
import org.jetbrains.plugins.gradle.importing.GradleSettingsImportingTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vladislav.Soroka
 */
public class GradleApplicationEnvironmentProviderTest extends GradleSettingsImportingTestCase {

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    getCurrentExternalProjectSettings().setDelegatedBuild(true);
  }

  @Test
  public void testApplicationRunConfigurationSettingsImport() throws Exception {
    PlatformTestUtil.getOrCreateProjectTestBaseDir(myProject);
    @Language("Java")
    String appClass = "package my;\n" +
                      "import java.util.Arrays;\n" +
                      "\n" +
                      "public class App {\n" +
                      "    public static void main(String[] args) {\n" +
                      "        System.out.println(\"Class-Path: \" + System.getProperty(\"java.class.path\"));\n" +
                      "        System.out.println(\"Passed arguments: \" + Arrays.toString(args));\n" +
                      "    }\n" +
                      "}\n";
    createProjectSubFile("src/main/java/my/App.java", appClass);
    createSettingsFile("rootProject.name = 'moduleName'");
    importProject(
      new GradleBuildScriptBuilderEx()
        .withJavaPlugin()
        .withIdeaPlugin()
        .withGradleIdeaExtPlugin(IDEA_EXT_PLUGIN_VERSION)
        .addImport("org.jetbrains.gradle.ext.*")
        .addPostfix("idea {\n" +
                    "  project.settings {\n" +
                    "    runConfigurations {\n" +
                    "       MyApp(Application) {\n" +
                    "           mainClass = 'my.App'\n" +
                    "           programParameters = 'foo --bar baz'\n" +
                    "           moduleName = 'moduleName.main'\n" +
                    "       }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}")
        .generate()
    );

    assertModules("moduleName", "moduleName.main", "moduleName.test");
    RunnerAndConfigurationSettings configurationSettings = RunManager.getInstance(myProject).findConfigurationByName("MyApp");
    ApplicationConfiguration configuration = (ApplicationConfiguration)configurationSettings.getConfiguration();

    String appArgs = "Passed arguments: [foo, --bar, baz]";
    System.out.println("Check ShortenCommandLine.NONE");
    configuration.setShortenCommandLine(ShortenCommandLine.NONE);
    assertAppRunOutput(configurationSettings, appArgs);

    System.out.println("Check ShortenCommandLine.MANIFEST");
    configuration.setShortenCommandLine(ShortenCommandLine.MANIFEST);
    assertAppRunOutput(configurationSettings, appArgs);

    Sdk jdk = JavaParametersUtil.createProjectJdk(myProject, configuration.getAlternativeJrePath());
    if (JavaSdk.getInstance().isOfVersionOrHigher(jdk, JavaSdkVersion.JDK_1_9)) {
      System.out.println("Check ShortenCommandLine.ARGS_FILE");
      configuration.setShortenCommandLine(ShortenCommandLine.ARGS_FILE);
      assertAppRunOutput(configurationSettings, appArgs);
    }
    else {
      System.out.println("Check ShortenCommandLine.CLASSPATH_FILE");
      configuration.setShortenCommandLine(ShortenCommandLine.CLASSPATH_FILE);
      assertAppRunOutput(configurationSettings, appArgs);
    }
  }

  private void assertAppRunOutput(RunnerAndConfigurationSettings configurationSettings, String... checks) {
    String output = runAppAndGetOutput(configurationSettings);
    for (String check : checks) {
      assertTrue(String.format("App output should contain substring: %s, but was:\n%s", check, output), output.contains(check));
    }
  }

  @NotNull
  private static String runAppAndGetOutput(RunnerAndConfigurationSettings configurationSettings) {
    final Semaphore done = new Semaphore();
    done.down();
    ExternalSystemProgressNotificationManager notificationManager =
      ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    StringBuilder out = new StringBuilder();
    ExternalSystemTaskNotificationListenerAdapter listener = new ExternalSystemTaskNotificationListenerAdapter() {
      private volatile ExternalSystemTaskId myId = null;

      @Override
      public void onStart(@NotNull ExternalSystemTaskId id, String workingDir) {
        if (myId != null) {
          throw new IllegalStateException("This test listener is not supposed to listen to more than 1 task");
        }
        myId = id;
      }

      @Override
      public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
        if (!id.equals(myId)) {
          return;
        }
        if (StringUtil.isEmptyOrSpaces(text)) return;
        (stdOut ? System.out : System.err).print(text);
        out.append(text);
      }

      @Override
      public void onEnd(@NotNull ExternalSystemTaskId id) {
        if (!id.equals(myId)) {
          return;
        }
        done.up();
      }
    };

    try {
      notificationManager.addNotificationListener(listener);
      edt(() -> {
        try {
          ExecutionEnvironment environment =
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), configurationSettings)
              .contentToReuse(null)
              .dataContext(null)
              .activeTarget()
              .build();
          ProgramRunnerUtil.executeConfiguration(environment, false, true);
        }
        catch (ExecutionException e) {
          fail(e.getMessage());
        }
      });
      Assert.assertTrue(done.waitFor(30000));
    }
    finally {
      notificationManager.removeNotificationListener(listener);
    }
    return out.toString();
  }
}