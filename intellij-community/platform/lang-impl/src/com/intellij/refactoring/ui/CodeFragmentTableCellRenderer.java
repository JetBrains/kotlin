/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.refactoring.ui;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiCodeFragment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author dsl
 */
public class CodeFragmentTableCellRenderer implements TableCellRenderer {
  private final Project myProject;
  private final FileType myFileType;

  public CodeFragmentTableCellRenderer(Project project) {
    this(project, StdFileTypes.JAVA);
  }

  public CodeFragmentTableCellRenderer(Project project, FileType fileType) {
    myProject = project;
    myFileType = fileType;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, final boolean hasFocus, int row, int column) {
    PsiCodeFragment codeFragment = (PsiCodeFragment)value;

    final EditorTextField editorTextField;
    Document document = null;
    if (codeFragment != null) {
      document = PsiDocumentManager.getInstance(myProject).getDocument(codeFragment);
      editorTextField = new EditorTextField(document, myProject, myFileType) {
        @Override
        protected boolean shouldHaveBorder() {
          return false;
        }
      };
    }
    else {
      editorTextField = new EditorTextField("", myProject, myFileType) {
        @Override
        protected boolean shouldHaveBorder() {
          return false;
        }
      };
    }

    if (!table.isShowing()) {
      editorTextField.ensureWillComputePreferredSize();
    }

    editorTextField.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
    editorTextField.setBorder((hasFocus || isSelected) ? BorderFactory.createLineBorder(table.getSelectionBackground()) : JBUI.Borders.empty(1));
    if (isSelected && document != null) {
      final Color bg = table.getSelectionBackground();
      final Color fg = table.getSelectionForeground();
      editorTextField.setBackground(bg);
      editorTextField.setForeground(fg);
      editorTextField.setAsRendererWithSelection(bg, fg);
    }
    return editorTextField;
  }
}
