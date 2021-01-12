// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.util;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.lang.findUsages.DescriptiveNameUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.FocusChangeListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementDescriptionUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.ui.EditorSettingsProvider;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author yole
 */
public final class RefactoringUIUtil {
  private RefactoringUIUtil() { }

  public static String getDescription(@NotNull PsiElement element, boolean includeParent) {
    RefactoringDescriptionLocation location = includeParent ? RefactoringDescriptionLocation.WITH_PARENT : RefactoringDescriptionLocation.WITHOUT_PARENT;
    return ElementDescriptionUtil.getElementDescription(element, location);
  }

  public static void processIncorrectOperation(Project project, IncorrectOperationException e) {
    String message = e.getMessage();
    int index = message != null ? message.indexOf("java.io.IOException") : -1;
    if (index > 0) {
      message = message.substring(index + "java.io.IOException".length());
    }

    String s = message;
    ApplicationManager.getApplication().invokeLater(
      () -> Messages.showMessageDialog(project, s, RefactoringBundle.message("error.title"), Messages.getErrorIcon()));
  }

  public static String calculatePsiElementDescriptionList(PsiElement[] elements) {
    Function<PsiElement, String> presentationFun = e -> UsageViewUtil.getType(e) + ' ' + DescriptiveNameUtil.getDescriptiveName(e);
    return StringUtil.join(ContainerUtil.map2LinkedSet(Arrays.asList(elements), presentationFun), ", ");
  }

  public static final EditorSettingsProvider SELECT_ALL_ON_FOCUS = editor -> editor.addFocusListener(new FocusChangeListener() {
    @Override
    public void focusGained(@NotNull Editor editor) {
      if (LookupManager.getActiveLookup(editor) == null) {
        editor.getSelectionModel().setSelection(0, editor.getDocument().getTextLength());
      }
    }
  });
}