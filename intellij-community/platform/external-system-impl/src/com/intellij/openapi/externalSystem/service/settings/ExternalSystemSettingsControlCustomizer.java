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

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemSettingsControlCustomizer {

  private boolean hideUseAutoImportBox;
  private boolean hideCreateEmptyContentRootDirectoriesBox;
  private boolean hideModulesGroupingOptionPanel;

  public ExternalSystemSettingsControlCustomizer() {
  }

  @Deprecated
  public ExternalSystemSettingsControlCustomizer(boolean hideUseAutoImportBox,
                                                 boolean hideCreateEmptyContentRootDirectoriesBox) {
    this(hideUseAutoImportBox);
  }

  @Deprecated
  public ExternalSystemSettingsControlCustomizer(boolean hideUseAutoImportBox,
                                                 boolean hideCreateEmptyContentRootDirectoriesBox,
                                                 boolean hideModulesGroupingOptionPanel) {
    this(hideUseAutoImportBox);
  }

  public ExternalSystemSettingsControlCustomizer(boolean hideUseAutoImportBox) {
    this.hideUseAutoImportBox = hideUseAutoImportBox;
  }

  public boolean isUseAutoImportBoxHidden() {
    return hideUseAutoImportBox;
  }

  @Deprecated
  public boolean isCreateEmptyContentRootDirectoriesBoxHidden() {
    return false;
  }

  @Deprecated
  public boolean isModulesGroupingOptionPanelHidden() {
    return false;
  }
}
