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
      if (!issue.isEnabledByDefault()) {
        // Lint will skip issues (and not report them) for issues that have been disabled,
        // except for those issues that are explicitly enabled via Gradle. Therefore, if
        // we get this far, lint has found this issue to be explicitly enabled, so we let
        // that setting override a local enabled/disabled state in the IDE profile.
      } else {
        return null;
      }
    }

    final AndroidLintInspectionBase inspection = (AndroidLintInspectionBase)profile.getUnwrappedTool(inspectionShortName, context);
    if (inspection == null) return null;
    final HighlightDisplayLevel errorLevel = profile.getErrorLevel(key, context);
    return Pair.create(inspection,
                       errorLevel != null ? errorLevel : HighlightDisplayLevel.WARNING);
  }
}
