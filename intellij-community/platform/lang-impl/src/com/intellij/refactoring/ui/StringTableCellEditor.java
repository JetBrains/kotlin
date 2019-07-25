/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;

/**
 * @author dsl
 */
public class StringTableCellEditor extends AbstractCellEditor implements TableCellEditor {
  private Document myDocument;
  private final Project myProject;
  private final List<DocumentListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public StringTableCellEditor(final Project project) {
    myProject = project;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    final EditorTextField editorTextField = new EditorTextField((String) value, myProject, StdFileTypes.JAVA) {
            @Override
            protected boolean shouldHaveBorder() {
              return false;
            }
          };
    myDocument = editorTextField.getDocument();
    if (myDocument != null) {
      for (DocumentListener listener : myListeners) {
        editorTextField.addDocumentListener(listener);
      }
    }
    return editorTextField;
  }

  @Override
  public Object getCellEditorValue() {
    return myDocument.getText();
  }

  public void addDocumentListener(DocumentListener listener) {
    myListeners.add(listener);
  }

  public void clearListeners() {
    myListeners.clear();
  }
}
