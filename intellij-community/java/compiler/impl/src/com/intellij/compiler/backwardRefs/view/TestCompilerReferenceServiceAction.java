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
package com.intellij.compiler.backwardRefs.view;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.compiler.CompilerReferenceService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TestCompilerReferenceServiceAction extends AnAction {
  public TestCompilerReferenceServiceAction(String text) {
    super(text);
  }

  @Override
  public final void actionPerformed(@NotNull AnActionEvent e) {
    final PsiElement element = getPsiElement(e.getDataContext());
    if (element != null) startActionFor(element);
  }

  protected abstract void startActionFor(@NotNull PsiElement element);

  protected abstract boolean canBeAppliedFor(@NotNull PsiElement element);

  @Override
  public final void update(@NotNull AnActionEvent e) {
    if (!CompilerReferenceService.isEnabled()) {
      e.getPresentation().setEnabled(false);
      return;
    }
    e.getPresentation().setEnabled(getPsiElement(e.getDataContext()) != null);
  }

  @Nullable
  private PsiElement getPsiElement(DataContext dataContext) {
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) return null;
    final PsiElement element = TargetElementUtil.getInstance().findTargetElement(editor,
                                                                                 TargetElementUtil.ELEMENT_NAME_ACCEPTED,
                                                                                 editor.getCaretModel().getOffset());
    if (element == null) return null;
    return canBeAppliedFor(element) ? element : null;

  }
}
