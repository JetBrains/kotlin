/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package com.intellij.ui;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * @author ven
 */
public class EditorComboWithBrowseButton extends ComponentWithBrowseButton<EditorComboBox> implements TextAccessor {
  public EditorComboWithBrowseButton(final ActionListener browseActionListener,
                                     final String text,
                                     @NotNull final Project project,
                                     final String recentsKey) {
    super(new EditorComboBox(text, project, StdFileTypes.PLAIN_TEXT), browseActionListener);
    final List<String> recentEntries = RecentsManager.getInstance(project).getRecentEntries(recentsKey);
    if (recentEntries != null) {
      setHistory(ArrayUtil.toStringArray(recentEntries));
    }
    if (text != null && text.length() > 0) {
      prependItem(text);
    }
  }

  @Override
  public String getText() {
    return getChildComponent().getText().trim();
  }

  @Override
  public void setText(final String text) {
    getChildComponent().setText(text);
  }

  public boolean isEditable() {
    return !getChildComponent().getEditorEx().isViewer();
  }

  public void setHistory(String[] history) {
    getChildComponent().setHistory(history);
  }

  public void prependItem(String item) {
    getChildComponent().prependItem(item);
  }
}