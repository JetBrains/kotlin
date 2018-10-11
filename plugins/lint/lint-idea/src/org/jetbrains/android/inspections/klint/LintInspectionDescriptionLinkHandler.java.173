/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.klint;

import com.android.tools.klint.checks.BuiltinIssueRegistry;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.TextFormat;
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

public class LintInspectionDescriptionLinkHandler extends TooltipLinkHandler {
  private static final Logger LOG = Logger.getInstance("#org.jetbrains.android.inspections.lint.LintInspectionDescriptionLinkHandler");

  @Override
  public String getDescription(@NotNull final String refSuffix, @NotNull final Editor editor) {
    final Project project = editor.getProject();
    if (project == null) {
      LOG.error(editor);
      return null;
    }

    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) {
      return null;
    }

    Issue issue = new BuiltinIssueRegistry().getIssue(refSuffix);
    if (issue != null) {
      String html = issue.getExplanation(TextFormat.HTML);
      // IntelliJ seems to treat newlines in the HTML as needing to also be converted to <br> (whereas
      // Lint includes these for HTML readability but they shouldn't add additional lines since it has
      // already added <br> as well) so strip these out
      html = html.replace("\n", "");
      return html;
    }

    // TODO: What about custom registries for custom rules, AARs etc?

    LOG.warn("No description for inspection '" + refSuffix + "'");
    return InspectionsBundle.message("inspection.tool.description.under.construction.text");
  }
}