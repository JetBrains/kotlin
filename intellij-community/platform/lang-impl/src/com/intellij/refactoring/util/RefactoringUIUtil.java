/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author yole
 */
public class RefactoringUIUtil {
  private RefactoringUIUtil() {
  }

  public static String getDescription(@NotNull PsiElement element, boolean includeParent) {
    return ElementDescriptionUtil.getElementDescription(element, includeParent
                                                                 ? RefactoringDescriptionLocation.WITH_PARENT
                                                                 : RefactoringDescriptionLocation.WITHOUT_PARENT);
  }

  public static void processIncorrectOperation(final Project project, IncorrectOperationException e) {
    @NonNls String message = e.getMessage();
    final int index = message != null ? message.indexOf("java.io.IOException") : -1;
    if (index > 0) {
      message = message.substring(index + "java.io.IOException".length());
    }

    final String s = message;
    ApplicationManager.getApplication().invokeLater(
      () -> Messages.showMessageDialog(project, s, RefactoringBundle.message("error.title"), Messages.getErrorIcon()));
  }

  public static String calculatePsiElementDescriptionList(PsiElement[] elements) {
    final Function<PsiElement, String> presentationFun = element -> UsageViewUtil.getType(element) +
                                                                    " " +
                                                                    DescriptiveNameUtil.getDescriptiveName(element);
    return StringUtil.join(ContainerUtil.map2LinkedSet(Arrays.asList(elements), presentationFun), ", ");
  }

  public static final EditorSettingsProvider SELECT_ALL_ON_FOCUS = editor -> editor.addFocusListener(new FocusChangeListener() {
    @Override
    public void focusGained(@NotNull Editor editor) {
      if (LookupManager.getActiveLookup(editor) == null) {
        editor.getSelectionModel().setSelection(0, editor.getDocument().getTextLength());
      }
    }

    @Override
    public void focusLost(@NotNull Editor editor) {
    }
  });
}
