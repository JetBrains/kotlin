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
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LanguageChangeSignatureDetector<C extends ChangeInfo> {

  @NotNull  C createInitialChangeInfo(final @NotNull PsiElement element);
  boolean ignoreChanges(PsiElement element);
  @Nullable C createNextChangeInfo(String signature, @NotNull C currentInfo, boolean delegate);

  void performChange(C changeInfo, Editor editor, @NotNull String oldText);

  boolean isChangeSignatureAvailableOnElement(@NotNull PsiElement element, C currentInfo);

  TextRange getHighlightingRange(@NotNull C changeInfo);

  default @NotNull String extractSignature(@NotNull C initialChangeInfo) {
    final TextRange signatureRange = getHighlightingRange(initialChangeInfo);
    return signatureRange.shiftRight(-signatureRange.getStartOffset()).substring(initialChangeInfo.getMethod().getText());
  }

  default String getMethodSignaturePreview(C info, final List<? super TextRange> deleteRanges, final List<? super TextRange> newRanges) {
    return extractSignature(info);
  }

  FileType getFileType();
}
