// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionsBundle;
import com.intellij.codeInspection.lang.InspectionExtensionsFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditInspectionToolsSettingsInSuppressedPlaceIntention implements IntentionAction {
  private String myId;
  private String myDisplayName;

  @Override
  @NotNull
  public String getFamilyName() {
    return InspectionsBundle.message("edit.options.of.reporter.inspection.family");
  }

  @Override
  @NotNull
  public String getText() {
    return InspectionsBundle.message("edit.inspection.options", myDisplayName);
  }

  @Nullable
  private static String getSuppressedId(Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    PsiElement element = file.findElementAt(offset);
    while (element != null && !(element instanceof PsiFile)) {
      for (InspectionExtensionsFactory factory : InspectionExtensionsFactory.EP_NAME.getExtensionList()) {
        final String suppressedIds = factory.getSuppressedInspectionIdsIn(element);
        if (suppressedIds != null) {
          for (String id : StringUtil.split(suppressedIds, ",")) {
            if (isCaretOnSuppressedId(file, offset, id)) {
              return id;
            }
          }
        }
      }
      element = element.getParent();
    }
    return null;
  }

  private static boolean isCaretOnSuppressedId(PsiFile file, int caretOffset, String suppressedId) {
    CharSequence fileText = file.getViewProvider().getContents();
    int start = Math.max(0, caretOffset - suppressedId.length());
    int end = Math.min(caretOffset + suppressedId.length(), fileText.length());
    return StringUtil.indexOf(fileText.subSequence(start, end), suppressedId) >= 0;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    myId = getSuppressedId(editor, file);
    if (myId != null) {
      InspectionToolWrapper toolWrapper = getTool(project, file);
      if (toolWrapper == null) return false;
      myDisplayName = toolWrapper.getDisplayName();
    }
    return myId != null;
  }

  @Nullable
  private InspectionToolWrapper getTool(final Project project, final PsiFile file) {
    final InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(project);
    final InspectionProfileImpl inspectionProfile = projectProfileManager.getCurrentProfile();
    return inspectionProfile.getToolById(myId, file);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    InspectionToolWrapper toolWrapper = getTool(project, file);
    if (toolWrapper == null) return;
    final InspectionProjectProfileManager projectProfileManager = InspectionProjectProfileManager.getInstance(project);
    final InspectionProfileImpl inspectionProfile = projectProfileManager.getCurrentProfile();
    EditInspectionToolsSettingsAction.editToolSettings(project, inspectionProfile, toolWrapper.getShortName());
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
