/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBRadioButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Templates class for managing single external project settings (single ide project might contain multiple bindings to external
 * projects, e.g. one module is backed by a single external project and couple of others are backed by a single external multi-project).
 * 
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalProjectSettingsControl<S extends ExternalProjectSettings>
  extends AbstractSettingsControl
  implements ExternalSystemSettingsControl<S>
{

  @NotNull private final S myInitialSettings;

  @Nullable
  private JBCheckBox myUseAutoImportBox;
  @Nullable
  private JBCheckBox myCreateEmptyContentRootDirectoriesBox;
  @Nullable
  private JBRadioButton myUseQualifiedModuleNamesRadioButton;
  @Nullable
  private JBRadioButton myUseModuleGroupsRadioButton;
  @NotNull
  private final ExternalSystemSettingsControlCustomizer myCustomizer;
  @SuppressWarnings("FieldCanBeLocal") // the field needed for the option rendering per linked project, see ExternalSystemUiUtil.showUi
  @Nullable
  private JPanel myOrganizeModuleNamesPanel;

  protected AbstractExternalProjectSettingsControl(@NotNull S initialSettings) {
    this(null, initialSettings, null);
  }

  protected AbstractExternalProjectSettingsControl(@Nullable Project project,
                                                   @NotNull S initialSettings,
                                                   @Nullable ExternalSystemSettingsControlCustomizer controlCustomizer) {
    super(project);
    myInitialSettings = initialSettings;
    myCustomizer = controlCustomizer == null ? new ExternalSystemSettingsControlCustomizer() : controlCustomizer;
  }

  @NotNull
  public S getInitialSettings() {
    return myInitialSettings;
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel canvas, int indentLevel) {
    if (!myCustomizer.isUseAutoImportBoxHidden()) {
      myUseAutoImportBox = new JBCheckBox(ExternalSystemBundle.message("settings.label.use.auto.import"));
      canvas.add(myUseAutoImportBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }
    if (!myCustomizer.isCreateEmptyContentRootDirectoriesBoxHidden()) {
      myCreateEmptyContentRootDirectoriesBox =
        new JBCheckBox(ExternalSystemBundle.message("settings.label.create.empty.content.root.directories"));
      canvas.add(myCreateEmptyContentRootDirectoriesBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    if (!myCustomizer.isModulesGroupingOptionPanelHidden()) {
      myOrganizeModuleNamesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      myOrganizeModuleNamesPanel.add(new JBLabel(ExternalSystemBundle.message("settings.label.group.modules")));
      myUseModuleGroupsRadioButton = new JBRadioButton(ExternalSystemBundle.message("settings.radio.button.use.module.groups"), true);
      myOrganizeModuleNamesPanel.add(myUseModuleGroupsRadioButton);
      myUseQualifiedModuleNamesRadioButton = new JBRadioButton(ExternalSystemBundle.message("settings.radio.button.use.qualified.name"));
      myOrganizeModuleNamesPanel.add(myUseQualifiedModuleNamesRadioButton);
      ButtonGroup group = new ButtonGroup();
      group.add(myUseModuleGroupsRadioButton);
      group.add(myUseQualifiedModuleNamesRadioButton);
      canvas.add(myOrganizeModuleNamesPanel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }
    fillExtraControls(canvas, indentLevel); 
  }
  
  protected abstract void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel);

  @Override
  public boolean isModified() {
    boolean result = false;
    if (!myCustomizer.isUseAutoImportBoxHidden() && myUseAutoImportBox != null) {
      result = myUseAutoImportBox.isSelected() != getInitialSettings().isUseAutoImport();
    }
    if (!myCustomizer.isCreateEmptyContentRootDirectoriesBoxHidden() && myCreateEmptyContentRootDirectoriesBox != null) {
      result = result || myCreateEmptyContentRootDirectoriesBox.isSelected() != getInitialSettings().isCreateEmptyContentRootDirectories();
    }
    if (!myCustomizer.isModulesGroupingOptionPanelHidden() && myUseQualifiedModuleNamesRadioButton != null) {
      result |= myUseQualifiedModuleNamesRadioButton.isSelected() != getInitialSettings().isUseQualifiedModuleNames();
    }
    return result || isExtraSettingModified();
  }

  protected abstract boolean isExtraSettingModified();

  @Override
  public void reset() {
    reset(false, null);
  }

  @Override
  public void reset(@Nullable Project project) {
    reset(false, project);
  }

  @Override
  public void reset(@Nullable WizardContext wizardContext) {
    reset(false, wizardContext, null);
  }

  public void reset(boolean isDefaultModuleCreation, @Nullable Project project) {
    reset(isDefaultModuleCreation, null, project);
  }

  public void reset(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext, @Nullable Project project) {
    super.reset(wizardContext, project);
    if (!myCustomizer.isUseAutoImportBoxHidden() && myUseAutoImportBox != null) {
      myUseAutoImportBox.setSelected(getInitialSettings().isUseAutoImport());
    }
    if(isDefaultModuleCreation) {
      if(myCreateEmptyContentRootDirectoriesBox != null) {
        myCreateEmptyContentRootDirectoriesBox.getParent().remove(myCreateEmptyContentRootDirectoriesBox);
        myCreateEmptyContentRootDirectoriesBox = null;
      }
    }
    if (!isDefaultModuleCreation && !myCustomizer.isCreateEmptyContentRootDirectoriesBoxHidden() && myCreateEmptyContentRootDirectoriesBox != null) {
      myCreateEmptyContentRootDirectoriesBox.setSelected(getInitialSettings().isCreateEmptyContentRootDirectories());
    }

    if (!myCustomizer.isModulesGroupingOptionPanelHidden() &&
        myUseModuleGroupsRadioButton != null &&
        myUseQualifiedModuleNamesRadioButton != null) {
      boolean useQualifiedModuleNames = getInitialSettings().isUseQualifiedModuleNames();
      myUseModuleGroupsRadioButton.setSelected(!useQualifiedModuleNames);
      myUseQualifiedModuleNamesRadioButton.setSelected(useQualifiedModuleNames);
    }
    resetExtraSettings(isDefaultModuleCreation, wizardContext);
  }

  protected abstract void resetExtraSettings(boolean isDefaultModuleCreation);

  protected void resetExtraSettings(boolean isDefaultModuleCreation, @Nullable WizardContext wizardContext) {
    resetExtraSettings(isDefaultModuleCreation);
  }

  @Override
  public void apply(@NotNull S settings) {
    settings.setModules(myInitialSettings.getModules());
    if (!myCustomizer.isUseAutoImportBoxHidden() && myUseAutoImportBox != null) {
      settings.setUseAutoImport(myUseAutoImportBox.isSelected());
    }

    if (!myCustomizer.isCreateEmptyContentRootDirectoriesBoxHidden() && myCreateEmptyContentRootDirectoriesBox != null) {
      settings.setCreateEmptyContentRootDirectories(myCreateEmptyContentRootDirectoriesBox.isSelected());
    }
    if (myInitialSettings.getExternalProjectPath() != null) {
      settings.setExternalProjectPath(myInitialSettings.getExternalProjectPath());
    }
    if (!myCustomizer.isModulesGroupingOptionPanelHidden() && myUseQualifiedModuleNamesRadioButton != null) {
      settings.setUseQualifiedModuleNames(myUseQualifiedModuleNamesRadioButton.isSelected());
    }
    applyExtraSettings(settings);
  }

  protected abstract void applyExtraSettings(@NotNull S settings);

  @Override
  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }
  
  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }

  public void updateInitialSettings() {
    if (!myCustomizer.isUseAutoImportBoxHidden() && myUseAutoImportBox != null) {
      myInitialSettings.setUseAutoImport(myUseAutoImportBox.isSelected());
    }

    if (!myCustomizer.isCreateEmptyContentRootDirectoriesBoxHidden() && myCreateEmptyContentRootDirectoriesBox != null) {
      myInitialSettings.setCreateEmptyContentRootDirectories(myCreateEmptyContentRootDirectoriesBox.isSelected());
    }
    if (!myCustomizer.isModulesGroupingOptionPanelHidden() && myUseQualifiedModuleNamesRadioButton != null) {
      myInitialSettings.setUseQualifiedModuleNames(myUseQualifiedModuleNamesRadioButton.isSelected());
    }
    updateInitialExtraSettings();
  }

  protected void updateInitialExtraSettings(){}

  /**
   * see {@linkplain AbstractImportFromExternalSystemControl#setCurrentProject(Project)}
   */
  public void setCurrentProject(@Nullable Project project) {
    setProject(project);
  }

  @Nullable
  @Override
  public Project getProject() {
    return super.getProject();
  }
}
