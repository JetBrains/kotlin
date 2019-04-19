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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.editor.HectorComponentPanel;
import com.intellij.openapi.editor.HectorComponentPanelsProvider;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.DialogUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ImportPopupHectorComponentProvider implements HectorComponentPanelsProvider {

  @Override
  public HectorComponentPanel createConfigurable(@NotNull final PsiFile file) {
    final Project project = file.getProject();
    final DaemonCodeAnalyzer analyzer = DaemonCodeAnalyzer.getInstance(project);
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final VirtualFile virtualFile = file.getVirtualFile();
    assert virtualFile != null;
    final boolean notInLibrary =
      !fileIndex.isInLibrary(virtualFile) || fileIndex.isInContent(virtualFile);

    return new HectorComponentPanel() {
      private JCheckBox myImportPopupCheckBox = new JCheckBox(EditorBundle.message("hector.import.popup.checkbox"));
      @Override
      public JComponent createComponent() {
        DialogUtil.registerMnemonic(myImportPopupCheckBox);
        return myImportPopupCheckBox;
      }

      @Override
      public boolean isModified() {
        return myImportPopupCheckBox.isSelected() != analyzer.isImportHintsEnabled(file);
      }

      @Override
      public void apply() throws ConfigurationException {
        analyzer.setImportHintsEnabled(file, myImportPopupCheckBox.isSelected());
      }

      @Override
      public void reset() {
        myImportPopupCheckBox.setSelected(analyzer.isImportHintsEnabled(file));
        myImportPopupCheckBox.setEnabled(analyzer.isAutohintsAvailable(file));
        myImportPopupCheckBox.setVisible(notInLibrary);
      }

      @Override
      public void disposeUIResources() {
        myImportPopupCheckBox = null;
      }
    };
  }

}