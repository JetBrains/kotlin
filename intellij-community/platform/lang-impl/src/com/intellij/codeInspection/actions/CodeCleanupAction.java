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
package com.intellij.codeInspection.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.application.options.schemes.SchemesCombo;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ex.GlobalInspectionContextBase;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager;

public class CodeCleanupAction extends CodeInspectionAction {

  public static final String CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME = "Code Cleanup Inspections";

  public CodeCleanupAction() {
    super("Code Cleanup", "Code Cleanup");
  }

  @Override
  protected void runInspections(Project project, AnalysisScope scope) {
    final InspectionProfile profile = myExternalProfile != null ? myExternalProfile : InspectionProjectProfileManager.getInstance(project)
      .getCurrentProfile();
    final InspectionManager managerEx = InspectionManager.getInstance(project);
    final GlobalInspectionContextBase globalContext = (GlobalInspectionContextBase)managerEx.createNewGlobalContext();
    globalContext.codeCleanup(scope, profile, getTemplatePresentation().getText(), null, false);
  }

  @Override
  protected String getHelpTopic() {
    return "reference.dialogs.cleanup.scope";
  }

  @Override
  protected ExternalProfilesComboboxAwareInspectionToolsConfigurable createConfigurable(ProjectInspectionProfileManager projectProfileManager,
                                                                                        SchemesCombo<InspectionProfileImpl> profilesCombo) {
    return new ExternalProfilesComboboxAwareInspectionToolsConfigurable(projectProfileManager, profilesCombo) {
      @Override
      protected boolean acceptTool(InspectionToolWrapper entry) {
        return super.acceptTool(entry) && entry.isCleanupTool();
      }

      @Override
      public String getDisplayName() {
        return CODE_CLEANUP_INSPECTIONS_DISPLAY_NAME;
      }
    };
  }
}
