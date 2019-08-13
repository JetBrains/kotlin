/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ui.configuration.ContentEntryEditor;
import com.intellij.openapi.roots.ui.configuration.ContentEntryTreeEditor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Eugene Zhuravlev
 */
public class ToggleExcludedStateAction extends ContentEntryEditingAction {
  private final ContentEntryTreeEditor myEntryTreeEditor;

  public ToggleExcludedStateAction(JTree tree, ContentEntryTreeEditor entryEditor) {
    super(tree);
    myEntryTreeEditor = entryEditor;
    final Presentation templatePresentation = getTemplatePresentation();
    templatePresentation.setText(ProjectBundle.message("module.toggle.excluded.action"));
    templatePresentation.setDescription(ProjectBundle.message("module.toggle.excluded.action.description"));
    templatePresentation.setIcon(AllIcons.Modules.ExcludeRoot);
  }

  @Override
  public boolean isSelected(@NotNull final AnActionEvent e) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    if (selectedFiles.length == 0) return false;

    return myEntryTreeEditor.getContentEntryEditor().isExcludedOrUnderExcludedDirectory(selectedFiles[0]);
  }

  @Override
  public void setSelected(@NotNull final AnActionEvent e, final boolean isSelected) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    assert selectedFiles.length != 0;

    ContentEntryEditor contentEntryEditor = myEntryTreeEditor.getContentEntryEditor();
    for (VirtualFile selectedFile : selectedFiles) {
      if (isSelected) {
        if (!contentEntryEditor.isExcludedOrUnderExcludedDirectory(selectedFile)) { // not excluded yet
          contentEntryEditor.addExcludeFolder(selectedFile);
        }
      }
      else {
        contentEntryEditor.removeExcludeFolder(selectedFile.getUrl());
      }
    }
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    super.update(e);
    final Presentation presentation = e.getPresentation();
    presentation.setText(ProjectBundle.message("module.toggle.excluded.action"));
  }
}
