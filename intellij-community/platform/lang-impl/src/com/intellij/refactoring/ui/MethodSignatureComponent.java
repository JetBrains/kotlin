/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class MethodSignatureComponent extends EditorTextField {
  public MethodSignatureComponent(String signature, Project project, FileType filetype) {
    super(createNonUndoableDocument(signature), project, filetype, true, false);
    setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
    setBackground(EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR));
  }

  private static Document createNonUndoableDocument(String text) {
    Document document = EditorFactory.getInstance().createDocument(text);
    UndoUtil.disableUndoFor(document);
    return document;
  }

  public void setSignature(String signature) {
    setText(signature);
    final EditorEx editor = (EditorEx)getEditor();
    if (editor != null) {
      editor.getScrollingModel().scrollVertically(0);
      editor.getScrollingModel().scrollHorizontally(0);
    }
  }

  @Override
  protected EditorEx createEditor() {
    EditorEx editor = super.createEditor();
    final String fileName = getFileName();
    if (fileName != null) {
      editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(getProject(), fileName));
    }
    editor.getSettings().setWhitespacesShown(false);
    editor.setHorizontalScrollbarVisible(true);
    editor.setVerticalScrollbarVisible(true);
    return editor;
  }

  @Nullable
  protected String getFileName() {
    return null;
  }
}
