/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.ConfigurableExtensionPointUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurable;
import com.intellij.profile.codeInspection.ui.ErrorsConfigurableProvider;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.function.Consumer;

public class EditInspectionToolsSettingsAction implements IntentionAction, Iconable, HighPriorityAction {
  private final String myShortName;

  public EditInspectionToolsSettingsAction(@NotNull HighlightDisplayKey key) {
    myShortName = key.toString();
  }

  @Override
  @NotNull
  public String getText() {
    return InspectionsBundle.message("edit.options.of.reporter.inspection.text");
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return InspectionsBundle.message("edit.options.of.reporter.inspection.family");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(file.getProject());
    InspectionProfileImpl inspectionProfile = projectProfileManager.getCurrentProfile();
    editToolSettings(project,
                     inspectionProfile,
                     myShortName);
  }

  public boolean editToolSettings(final Project project,
                                  final InspectionProfileImpl inspectionProfile) {
    return editToolSettings(project,
                            inspectionProfile,
                            myShortName);
  }

  public static boolean editToolSettings(final Project project,
                                         final InspectionProfileImpl inspectionProfile,
                                         final String selectedToolShortName) {
    return editSettings(project, inspectionProfile, c -> c.selectInspectionTool(selectedToolShortName));
  }

  public static boolean editSettings(final Project project,
                                     final InspectionProfileImpl inspectionProfile,
                                     final Consumer<? super ErrorsConfigurable> configurableAction) {
    final ShowSettingsUtil settingsUtil = ShowSettingsUtil.getInstance();
    final ErrorsConfigurable errorsConfigurable = (ErrorsConfigurable) ConfigurableExtensionPointUtil.createProjectConfigurableForProvider(project, ErrorsConfigurableProvider.class);
    return settingsUtil.editConfigurable(project, errorsConfigurable, () -> {
      errorsConfigurable.selectProfile(inspectionProfile); // profile can be selected only after the UI has been initialized
      configurableAction.accept(errorsConfigurable);
    });
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  public Icon getIcon(int flags) {
    return AllIcons.General.Settings;
  }
}
