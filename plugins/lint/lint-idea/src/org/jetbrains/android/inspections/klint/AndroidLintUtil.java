/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.android.inspections.klint;

import com.android.tools.klint.detector.api.Issue;
import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidLintUtil {
  @NonNls static final String ATTR_VALUE_VERTICAL = "vertical";
  @NonNls static final String ATTR_VALUE_WRAP_CONTENT = "wrap_content";
  @NonNls static final String ATTR_LAYOUT_HEIGHT = "layout_height";
  @NonNls static final String ATTR_LAYOUT_WIDTH = "layout_width";
  @NonNls static final String ATTR_ORIENTATION = "orientation";

  private AndroidLintUtil() {
  }

  @Nullable
  public static Pair<AndroidLintInspectionBase, HighlightDisplayLevel> getHighlighLevelAndInspection(@NotNull Project project,
                                                                                                     @NotNull Issue issue,
                                                                                                     @NotNull PsiElement context) {
    final String inspectionShortName = AndroidLintInspectionBase.getInspectionShortNameByIssue(project, issue);
    if (inspectionShortName == null) {
      return null;
    }

    final HighlightDisplayKey key = HighlightDisplayKey.find(inspectionShortName);
    if (key == null) {
      return null;
    }

    final InspectionProfile profile = InspectionProjectProfileManager.getInstance(context.getProject()).getInspectionProfile();
    if (!profile.isToolEnabled(key, context)) {
      return null;
    }

    final AndroidLintInspectionBase inspection = (AndroidLintInspectionBase)profile.getUnwrappedTool(inspectionShortName, context);
    if (inspection == null) return null;
    final HighlightDisplayLevel errorLevel = profile.getErrorLevel(key, context);
    return Pair.create(inspection,
                       errorLevel != null ? errorLevel : HighlightDisplayLevel.WARNING);
  }
}
