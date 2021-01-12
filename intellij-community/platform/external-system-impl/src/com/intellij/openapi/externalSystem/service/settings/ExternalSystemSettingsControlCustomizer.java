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
package com.intellij.openapi.externalSystem.service.settings;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Vladislav.Soroka
 * @deprecated Useless class
 */
@Deprecated
@ApiStatus.ScheduledForRemoval(inVersion = "2021.1")
public class ExternalSystemSettingsControlCustomizer {

  private boolean hideUseAutoImportBox;
  private boolean hideCreateEmptyContentRootDirectoriesBox;
  private boolean hideModulesGroupingOptionPanel;

  public ExternalSystemSettingsControlCustomizer() {
  }

  public ExternalSystemSettingsControlCustomizer(boolean hideUseAutoImportBox,
                                                 boolean hideCreateEmptyContentRootDirectoriesBox) {
    this(hideUseAutoImportBox);
  }

  public ExternalSystemSettingsControlCustomizer(boolean hideUseAutoImportBox,
                                                 boolean hideCreateEmptyContentRootDirectoriesBox,
                                                 boolean hideModulesGroupingOptionPanel) {
    this(hideUseAutoImportBox);
  }

  public ExternalSystemSettingsControlCustomizer(boolean hideUseAutoImportBox) {
  }

  /**
   * @deprecated see {@link com.intellij.openapi.externalSystem.settings.ExternalProjectSettings#setUseAutoImport} for details
   */
  @Deprecated
  public boolean isUseAutoImportBoxHidden() {
    return true;
  }

  public boolean isCreateEmptyContentRootDirectoriesBoxHidden() {
    return false;
  }

  public boolean isModulesGroupingOptionPanelHidden() {
    return false;
  }
}
