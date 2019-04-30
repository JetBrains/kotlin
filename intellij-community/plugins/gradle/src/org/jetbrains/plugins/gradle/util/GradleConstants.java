package org.jetbrains.plugins.gradle.util;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class GradleConstants {

  @NotNull @NonNls public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("GRADLE");

  @NotNull @NonNls public static final String EXTENSION           = "gradle";
  @NotNull @NonNls public static final String DEFAULT_SCRIPT_NAME = "build.gradle";
  @NotNull @NonNls public static final String KOTLIN_DSL_SCRIPT_NAME = "build.gradle.kts";
  @NotNull @NonNls public static final String KOTLIN_DSL_SCRIPT_EXTENSION = "gradle.kts";
  @NotNull @NonNls public static final String SETTINGS_FILE_NAME  = "settings.gradle";
  @NotNull @NonNls public static final String KOTLIN_DSL_SETTINGS_FILE_NAME  = "settings.gradle.kts";

  @NotNull @NonNls public static final String[] BUILD_FILE_EXTENSIONS = {EXTENSION, KOTLIN_DSL_SCRIPT_EXTENSION};

  @NotNull public static final Set<String> KNOWN_GRADLE_FILES = ContainerUtil.immutableSet(DEFAULT_SCRIPT_NAME,
                                                                                           KOTLIN_DSL_SCRIPT_NAME,
                                                                                           SETTINGS_FILE_NAME,
                                                                                           KOTLIN_DSL_SETTINGS_FILE_NAME);

  @NotNull @NonNls public static final String SYSTEM_DIRECTORY_PATH_KEY = "GRADLE_USER_HOME";

  @NotNull @NonNls public static final String TOOL_WINDOW_TOOLBAR_PLACE = "GRADLE_SYNC_CHANGES_TOOLBAR";

  @NotNull @NonNls public static final String HELP_TOPIC_TOOL_WINDOW = "reference.toolwindows.gradle";

  @NotNull @NonNls public static final String OFFLINE_MODE_CMD_OPTION = "--offline";
  @NotNull @NonNls public static final String INIT_SCRIPT_CMD_OPTION = "--init-script";
  @NotNull @NonNls public static final String INCLUDE_BUILD_CMD_OPTION = "--include-build";

  @NotNull @NonNls public static final String GRADLE_SOURCE_SET_MODULE_TYPE_KEY = "sourceSet";
  public static final String TESTS_ARG_NAME = "--tests";

  public static final Key<Boolean> RUN_TASK_AS_TEST = Key.create("plugins.gradle.enable.test.reporting");

  private GradleConstants() {
  }
}
