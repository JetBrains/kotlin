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
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.actions.CodeCleanupAction;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;
import com.intellij.profile.codeInspection.ui.ProjectInspectionToolsConfigurable;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class EditCleanupProfileIntentionAction implements IntentionAction {
  public static final EditCleanupProfileIntentionAction INSTANCE = new EditCleanupProfileIntentionAction();
  private EditCleanupProfileIntentionAction() {}

  @Override
  @NotNull
  public String getText() {
    return getFamilyName();
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return "Edit cleanup profile settings";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final ProjectInspectionProfileManager profileManager = ProjectInspectionProfileManager.getInstance(project);
    final ProjectInspectionToolsConfigurable configurable =
      new ProjectInspectionToolsConfigurable(profileManager) {
        @Override
        protected boolean acceptTool(InspectionToolWrapper entry) {
          return super.acceptTool(entry) && entry.isCleanupTool();
        }

        @Override
        public String getDisplayName() {
          return CodeCleanupAction.CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME;
        }
      };
    ShowSettingsUtil.getInstance().editConfigurable(project, configurable);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
