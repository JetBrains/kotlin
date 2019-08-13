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
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.fileChooser.ex.FileNodeDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public abstract class ContentEntryEditingAction extends ToggleAction implements DumbAware {
  protected final JTree myTree;

  protected ContentEntryEditingAction(JTree tree) {
    myTree = tree;
    getTemplatePresentation().setEnabled(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    final Presentation presentation = e.getPresentation();
    presentation.setEnabled(true);
    final VirtualFile[] files = getSelectedFiles();
    if (files.length == 0) {
      presentation.setEnabled(false);
      return;
    }
    for (VirtualFile file : files) {
      if (file == null || !file.isDirectory()) {
        presentation.setEnabled(false);
        break;
      }
    }
  }

  @NotNull
  protected final VirtualFile[] getSelectedFiles() {
    final TreePath[] selectionPaths = myTree.getSelectionPaths();
    if (selectionPaths == null) {
      return VirtualFile.EMPTY_ARRAY;
    }
    final List<VirtualFile> selected = new ArrayList<>();
    for (TreePath treePath : selectionPaths) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePath.getLastPathComponent();
      final Object nodeDescriptor = node.getUserObject();
      if (!(nodeDescriptor instanceof FileNodeDescriptor)) {
        return VirtualFile.EMPTY_ARRAY;
      }
      final FileElement fileElement = ((FileNodeDescriptor)nodeDescriptor).getElement();
      final VirtualFile file = fileElement.getFile();
      if (file != null) {
        selected.add(file);
      }
    }
    return selected.toArray(VirtualFile.EMPTY_ARRAY);
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }
}
