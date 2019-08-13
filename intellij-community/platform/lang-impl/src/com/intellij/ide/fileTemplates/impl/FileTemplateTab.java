/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.ide.fileTemplates.impl;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexey Kudravtsev
 */
abstract class FileTemplateTab {
  protected final List<FileTemplateBase> myTemplates = new ArrayList<>();
  private final String myTitle;
  protected static final Color MODIFIED_FOREGROUND = JBColor.BLUE;

  protected FileTemplateTab(String title) {
    myTitle = title;
  }

  public abstract JComponent getComponent();

  @Nullable
  public abstract FileTemplate getSelectedTemplate();

  public abstract void selectTemplate(FileTemplate template);

  public abstract void removeSelected();
  public abstract void onTemplateSelected();

  public void init(FileTemplate[] templates) {
    final FileTemplate oldSelection = getSelectedTemplate();
    final String oldSelectionName = oldSelection != null? ((FileTemplateBase)oldSelection).getQualifiedName() : null;

    myTemplates.clear();
    FileTemplate newSelection = null;
    for (FileTemplate original : templates) {
      final FileTemplateBase copy = (FileTemplateBase)original.clone();
      if (oldSelectionName != null && oldSelectionName.equals(copy.getQualifiedName())) {
        newSelection = copy;
      }
      myTemplates.add(copy);
    }
    initSelection(newSelection);
  }

  protected abstract void initSelection(FileTemplate selection);

  public abstract void fireDataChanged();

  @NotNull 
  public FileTemplate[] getTemplates() {
    return myTemplates.toArray(FileTemplate.EMPTY_ARRAY);
  }

  public abstract void addTemplate(FileTemplate newTemplate);

  public String getTitle() {
    return myTitle;
  }

}
