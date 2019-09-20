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
package com.intellij.refactoring.changeSignature.inplace;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.BaseRefactoringIntentionAction;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ApplyChangeSignatureAction extends BaseRefactoringIntentionAction {
  public static final String CHANGE_SIGNATURE = "Apply signature change";
  private final String myMethodName;

  public ApplyChangeSignatureAction(String methodName) {
    myMethodName = methodName;
  }

  @NotNull
  @Override
  public String getText() {
    return RefactoringBundle.message("changing.signature.of.0", myMethodName);
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return CHANGE_SIGNATURE;
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
    final LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetectors.INSTANCE.forLanguage(element.getLanguage());
    if (detector != null) {
      InplaceChangeSignature changeSignature = InplaceChangeSignature.getCurrentRefactoring(editor);
      ChangeInfo currentInfo = changeSignature != null ? changeSignature.getCurrentInfo() : null;
      if (currentInfo != null && detector.isChangeSignatureAvailableOnElement(element, currentInfo)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
    InplaceChangeSignature signatureGestureDetector = InplaceChangeSignature.getCurrentRefactoring(editor);
    final String initialSignature = signatureGestureDetector.getInitialSignature();
    final ChangeInfo currentInfo = signatureGestureDetector.getCurrentInfo();
    signatureGestureDetector.detach();

    final LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetectors.INSTANCE.forLanguage(element.getLanguage());

    detector.performChange(currentInfo, editor, initialSignature);
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
    return file;
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }
}
