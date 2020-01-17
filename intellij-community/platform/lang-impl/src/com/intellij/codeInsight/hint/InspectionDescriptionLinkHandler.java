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
package com.intellij.codeInsight.hint;

import com.intellij.codeInsight.highlighting.TooltipLinkHandler;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Handles tooltip links in format {@code #inspection/inspection_short_name}.
 * On a click or expend acton returns more detailed description for given inspection.
 *
 * @author peter
 */
public class InspectionDescriptionLinkHandler extends TooltipLinkHandler {
  private static final Logger LOG = Logger.getInstance(InspectionDescriptionLinkHandler.class);

  @Override
  public String getDescription(@NotNull final String refSuffix, @NotNull final Editor editor) {
    final Project project = editor.getProject();
    if (project == null) {
      LOG.error(editor);
      return null;
    }
    if (project.isDisposed()) return null;
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      return null;
    }

    final InspectionProfile profile = InspectionProfileManager.getInstance().getCurrentProfile();
    final InspectionToolWrapper toolWrapper = profile.getInspectionTool(refSuffix, file);
    if (toolWrapper == null) return null;

    String description = toolWrapper.loadDescription();
    if (description == null) {
      LOG.warn("No description for inspection '" + refSuffix + "'");
      description = InspectionsBundle.message("inspection.tool.description.under.construction.text");
    }
    return description;
  }
}
