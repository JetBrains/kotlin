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

package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.copy.CopyHandler;
import org.jetbrains.annotations.NotNull;

public class CopyElementAction extends AnAction {

  @Override
  public boolean startInTransaction() {
    return true;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;
    }

    CommandProcessor.getInstance().executeCommand(project, () -> PsiDocumentManager.getInstance(project).commitAllDocuments(), "", null
    );
    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    PsiElement[] elements;

    PsiElement targetPsiElement = LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext);
    PsiDirectory defaultTargetDirectory = targetPsiElement instanceof PsiDirectory ? (PsiDirectory)targetPsiElement : null;
    if (editor != null) {
      PsiElement aElement = getTargetElement(editor, project);
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return;
      elements = new PsiElement[]{aElement};
      if (aElement == null || !CopyHandler.canCopy(elements)) {
        elements = new PsiElement[]{file};
      }
    }
    else {
      elements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext);
    }
    doCopy(elements, defaultTargetDirectory);
  }

  protected void doCopy(PsiElement[] elements, PsiDirectory defaultTargetDirectory) {
    CopyHandler.doCopy(elements, defaultTargetDirectory);
  }

  @Override
  public void update(@NotNull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    presentation.setEnabled(false);
    if (project == null) {
      return;
    }

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor != null) {
      updateForEditor(dataContext, presentation);
    }
    else {
      String id = ToolWindowManager.getInstance(project).getActiveToolWindowId();
      updateForToolWindow(id, dataContext, presentation);
    }
  }

  protected void updateForEditor(DataContext dataContext, Presentation presentation) {
    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) {
      presentation.setVisible(false);
      return;
    }

    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;

    }
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());

    PsiElement element = getTargetElement(editor, project);
    boolean result = element != null && CopyHandler.canCopy(new PsiElement[]{element});

    if (!result && file != null) {
      result = CopyHandler.canCopy(new PsiElement[]{file});
    }

    presentation.setEnabled(result);
    presentation.setVisible(true);
  }

  protected void updateForToolWindow(String toolWindowId, DataContext dataContext,Presentation presentation) {
    PsiElement[] elements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext);
    presentation.setEnabled(elements != null && CopyHandler.canCopy(elements));
  }

  private static PsiElement getTargetElement(final Editor editor, final Project project) {
    int offset = editor.getCaretModel().getOffset();
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return null;
    PsiElement element = file.findElementAt(offset);
    if (element == null) element = file;
    return element;
  }
}
