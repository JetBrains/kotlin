/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class HighlightUsagesHandlerFactoryBase implements HighlightUsagesHandlerFactory {
  @Nullable
  @Override
  public final HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file) {
    PsiElement target = findTarget(editor, file);
    if (target == null) return null;
    return createHighlightUsagesHandler(editor, file, target);
  }

  @Nullable
  @Override
  public final HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file,
                                                                       @NotNull ProperTextRange visibleRange) {
    PsiElement target = findTarget(editor, file);
    if (target == null) return null;
    return createHighlightUsagesHandler(editor, file, target, visibleRange);
  }

  @Nullable
  public abstract HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement target);

  @Nullable
  public HighlightUsagesHandlerBase createHighlightUsagesHandler(@NotNull Editor editor, @NotNull PsiFile file, @NotNull PsiElement target,
                                                                 @NotNull ProperTextRange visibleRange) {
    return createHighlightUsagesHandler(editor, file, target);
  }

  private static PsiElement findTarget(@NotNull Editor editor, @NotNull PsiFile file) {
    int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
    return file.findElementAt(offset);
  }
}
