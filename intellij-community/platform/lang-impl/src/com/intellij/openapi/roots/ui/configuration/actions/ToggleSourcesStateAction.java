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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.ui.configuration.ContentEntryEditor;
import com.intellij.openapi.roots.ui.configuration.ContentEntryTreeEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleSourceRootEditHandler;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;

import javax.swing.*;
import java.util.Locale;

/**
 * @author Eugene Zhuravlev
 */
public class ToggleSourcesStateAction<P extends JpsElement> extends ContentEntryEditingAction {
  private final ContentEntryTreeEditor myEntryTreeEditor;
  private final ModuleSourceRootEditHandler<P> myEditHandler;

  public ToggleSourcesStateAction(JTree tree, ContentEntryTreeEditor entryEditor, ModuleSourceRootEditHandler<P> editHandler) {
    super(tree);
    myEntryTreeEditor = entryEditor;
    myEditHandler = editHandler;
    final Presentation templatePresentation = getTemplatePresentation();
    templatePresentation.setText(editHandler.getMarkRootButtonText());
    templatePresentation.setDescription(ProjectBundle.message("module.toggle.sources.action.description",
                                                              editHandler.getFullRootTypeName().toLowerCase(Locale.getDefault())));
    templatePresentation.setIcon(editHandler.getRootIcon());
  }

  @Override
  public boolean isSelected(@NotNull final AnActionEvent e) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    if (selectedFiles.length == 0) return false;

    final ContentEntryEditor editor = myEntryTreeEditor.getContentEntryEditor();
    return myEditHandler.getRootType().equals(editor.getRootType(selectedFiles[0]));
  }

  @Override
  public void setSelected(@NotNull final AnActionEvent e, final boolean isSelected) {
    final VirtualFile[] selectedFiles = getSelectedFiles();
    assert selectedFiles.length != 0;

    final ContentEntryEditor contentEntryEditor = myEntryTreeEditor.getContentEntryEditor();
    for (VirtualFile selectedFile : selectedFiles) {
      final SourceFolder sourceFolder = contentEntryEditor.getSourceFolder(selectedFile);
      if (isSelected) {
        if (sourceFolder == null) { // not marked yet
          P properties = myEditHandler.getRootType().createDefaultProperties();
          contentEntryEditor.addSourceFolder(selectedFile, myEditHandler.getRootType(), properties);
        }
        else if (!myEditHandler.getRootType().equals(sourceFolder.getRootType())) {
          P properties;
          if (myEditHandler.getRootType().getClass().equals(sourceFolder.getRootType().getClass())) {
            properties = (P)sourceFolder.getJpsElement().getProperties().getBulkModificationSupport().createCopy();
          }
          else {
            properties = myEditHandler.getRootType().createDefaultProperties();
          }
          contentEntryEditor.removeSourceFolder(sourceFolder);
          contentEntryEditor.addSourceFolder(selectedFile, myEditHandler.getRootType(), properties);
        }
      }
      else if (sourceFolder != null) { // already marked
        contentEntryEditor.removeSourceFolder(sourceFolder);
      }
    }
  }
}
