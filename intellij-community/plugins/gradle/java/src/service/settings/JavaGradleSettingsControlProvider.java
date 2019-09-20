// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

/**
 * @author Vladislav.Soroka
 */
public class JavaGradleSettingsControlProvider extends GradleSettingsControlProvider {
  @Override
  public String getPlatformPrefix() {
    return PlatformUtils.isIntelliJ() ? PlatformUtils.getPlatformPrefix() : PlatformUtils.IDEA_CE_PREFIX;
  }

  @Override
  public GradleSystemSettingsControlBuilder getSystemSettingsControlBuilder(@NotNull GradleSettings initialSettings) {
    IdeaGradleSystemSettingsControlBuilder result = new IdeaGradleSystemSettingsControlBuilder(initialSettings);
    if (initialSettings.getProject().isDefault()) result.dropStoreExternallyCheckBox();
    return result;
  }

  @Override
  public GradleProjectSettingsControlBuilder getProjectSettingsControlBuilder(@NotNull GradleProjectSettings initialSettings) {
    return new JavaGradleProjectSettingsControlBuilder(initialSettings)
      // Hide bundled distribution option for a while
      .dropUseBundledDistributionButton();
  }
}
