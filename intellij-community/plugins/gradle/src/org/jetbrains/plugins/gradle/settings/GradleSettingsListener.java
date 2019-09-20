package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines callback for the gradle config structure change.
 * <p/>
 * Implementations of this interface are not obliged to be thread-safe.
 * 
 * @author Denis Zhdanov
 */
public interface GradleSettingsListener extends ExternalSystemSettingsListener<GradleProjectSettings> {

  Topic<GradleSettingsListener> TOPIC = Topic.create("Gradle-specific settings", GradleSettingsListener.class);

  /**
   * Is expected to be invoked when gradle home path is changed.
   * <p/>
   * <b>Note:</b> this callback is executed <b>after</b> the actual config change.
   *
   * @param oldPath            old path (if any)
   * @param newPath            new path (if any)
   * @param linkedProjectPath  target linked gradle project path
   */
  void onGradleHomeChange(@Nullable String oldPath, @Nullable String newPath, @NotNull String linkedProjectPath);

  /**
   * Is expected to be invoked when 'gradle distribution type' setting is changed (generally this
   * switches tooling api to different gradle version).
   * <p/>
   * <b>Note:</b> this callback is executed <b>after</b> the actual config change.
   *
   * @param currentValue       current value
   * @param linkedProjectPath  target linked gradle project path
   */
  void onGradleDistributionTypeChange(DistributionType currentValue, @NotNull String linkedProjectPath);

  /**
   * Is expected to be invoked when service directory path is changed.
   * <p/>
   * <b>Note:</b> this callback is executed <b>after</b> the actual config change.
   *
   * @param oldPath  old path (if any)
   * @param newPath  new path (if any)
   * @see GradleSettings#getServiceDirectoryPath() 
   */
  void onServiceDirectoryPathChange(@Nullable String oldPath, @Nullable String newPath);

  /**
   * Is expected to be called when gradle JVM options are changed by end-user.
   * 
   * @param oldOptions  old options (if any)
   * @param newOptions  new option (if any)
   */
  void onGradleVmOptionsChange(@Nullable String oldOptions, @Nullable String newOptions);

  /**
   * Is expected to be called when build delegation setting is changed by end-user.
   *
   * @param delegatedBuild    current value
   * @param linkedProjectPath target linked gradle project path
   */
  void onBuildDelegationChange(boolean delegatedBuild, @NotNull String linkedProjectPath);

  /**
   * Is expected to be called when test runner setting is changed by end-user.
   *
   * @param currentTestRunner current value
   * @param linkedProjectPath target linked gradle project path
   */
  void onTestRunnerChange(@NotNull TestRunner currentTestRunner, @NotNull String linkedProjectPath);
}
