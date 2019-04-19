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

package com.intellij.codeEditor.printing;

import com.intellij.CommonBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.FileNotFoundException;

public class ExportToHTMLAction extends AnAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return;
    }
    try {
      ExportToHTMLManager.executeExport(dataContext);
    }
    catch (FileNotFoundException ex) {
      JOptionPane.showMessageDialog(null, CodeEditorBundle.message("file.not.found", ex.getMessage()),
                                    CommonBundle.getErrorTitle(), JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    DataContext dataContext = event.getDataContext();
    PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
    if(psiElement instanceof PsiDirectory) {
      presentation.setEnabled(true);
      return;
    }
    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
    presentation.setEnabled(psiFile != null && psiFile.getContainingDirectory() != null);
    presentation.setVisible(psiFile != null);
  }

}