// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated use {@link GradleProjectSettings}
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2020.1")
public class GradleSystemRunningSettings {
  private static final Logger LOG = Logger.getInstance(GradleSystemRunningSettings.class);
  private static boolean alreadyLogged = false;

  @NotNull
  @Deprecated
  public PreferredTestRunner getDefaultTestRunner() {
    return PreferredTestRunner.PLATFORM_TEST_RUNNER;
  }

  /**
   * @deprecated use {@link GradleProjectSettings#isDelegatedBuildEnabled(Module)} )
   */
  @Deprecated
  public boolean isUseGradleAwareMake() {
    return false;
  }

  @NotNull
  @Deprecated
  public static GradleSystemRunningSettings getInstance() {
    if (!alreadyLogged) {
      LOG.warn("This class is deprecated please migrate to GradleProjectSettings");
      alreadyLogged = true;
    }
    return new GradleSystemRunningSettings();
  }

  @Deprecated
  public enum PreferredTestRunner {
    PLATFORM_TEST_RUNNER, GRADLE_TEST_RUNNER, CHOOSE_PER_TEST
  }
}