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
package org.jetbrains.plugins.gradle.service.settings;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

/**
 * @author Vladislav.Soroka
 */
public abstract class GradleSettingsControlProvider {

  private static final ExtensionPointName<GradleSettingsControlProvider> EP_NAME =
    ExtensionPointName.create("org.jetbrains.plugins.gradle.settingsControlProvider");

  public abstract String getPlatformPrefix();

  public abstract GradleSystemSettingsControlBuilder getSystemSettingsControlBuilder(@NotNull GradleSettings initialSettings);

  public abstract GradleProjectSettingsControlBuilder getProjectSettingsControlBuilder(@NotNull GradleProjectSettings initialSettings);

  @NotNull
  public static GradleSettingsControlProvider get() {
    GradleSettingsControlProvider result = null;
    final String platformPrefix = PlatformUtils.getPlatformPrefix();
    for (GradleSettingsControlProvider provider : EP_NAME.getExtensions()) {
      if (StringUtil.equals(platformPrefix, provider.getPlatformPrefix())) {
        assert result == null : "Multiple GradleSettingsControlProvider extensions found";
        result = provider;
      }
    }
    return ObjectUtils.notNull(result, new GradleSettingsControlProvider() {
      @Override
      public String getPlatformPrefix() {
        return null;
      }

      @Override
      public GradleSystemSettingsControlBuilder getSystemSettingsControlBuilder(@NotNull GradleSettings initialSettings) {
        return new IdeaGradleSystemSettingsControlBuilder(initialSettings).
          // always use external storage for project files
          dropStoreExternallyCheckBox();
      }

      @Override
      public GradleProjectSettingsControlBuilder getProjectSettingsControlBuilder(@NotNull GradleProjectSettings initialSettings) {
        return new IdeaGradleProjectSettingsControlBuilder(initialSettings)
          // hide java-specific option
          .dropResolveModulePerSourceSetCheckBox()
          .dropDelegateBuildCombobox()
          .dropTestRunnerCombobox()
          // hide this confusing option
          .dropCustomizableWrapperButton()
          // Hide bundled distribution option for a while
          .dropUseBundledDistributionButton();
      }
    });
  }
}
