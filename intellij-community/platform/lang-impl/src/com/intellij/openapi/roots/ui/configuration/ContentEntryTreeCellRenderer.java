/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.jps.model.java.JavaResourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.module.JpsModuleSourceRoot;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class ContentEntryTreeCellRenderer extends NodeRenderer {
  protected final ContentEntryTreeEditor myTreeEditor;
  private final List<? extends ModuleSourceRootEditHandler<?>> myEditHandlers;

  public ContentEntryTreeCellRenderer(@NotNull final ContentEntryTreeEditor treeEditor, List<? extends ModuleSourceRootEditHandler<?>> editHandlers) {
    myTreeEditor = treeEditor;
    myEditHandlers = editHandlers;
  }

  @Override
  public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

    final ContentEntryEditor editor = myTreeEditor.getContentEntryEditor();
    if (editor != null) {
      final Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
      if (userObject instanceof NodeDescriptor) {
        final Object element = ((NodeDescriptor)userObject).getElement();
        if (element instanceof FileElement) {
          final VirtualFile file = ((FileElement)element).getFile();
          if (file != null && file.isDirectory()) {
            final ContentEntry contentEntry = editor.getContentEntry();
            if (contentEntry != null) {
              final String prefix = getPresentablePrefix(contentEntry, file);
              if (!prefix.isEmpty()) {
                append(" (" + prefix + ")", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.GRAY));
              }
              setIcon(updateIcon(contentEntry, file, getIcon()));
            }
          }
        }
      }
    }
  }

  private static String getPresentablePrefix(final ContentEntry entry, final VirtualFile file) {
    for (final SourceFolder sourceFolder : entry.getSourceFolders()) {
      if (file.equals(sourceFolder.getFile())) {
        JpsModuleSourceRoot element = sourceFolder.getJpsElement();
        JavaSourceRootProperties properties = element.getProperties(JavaModuleSourceRootTypes.SOURCES);
        if (properties != null) return properties.getPackagePrefix();
        JavaResourceRootProperties resourceRootProperties = element.getProperties(JavaModuleSourceRootTypes.RESOURCES);
        if (resourceRootProperties != null) return resourceRootProperties.getRelativeOutputPath();
      }
    }
    return "";
  }

  protected Icon updateIcon(final ContentEntry entry, final VirtualFile file, Icon originalIcon) {
    if (ContentEntryEditor.isExcludedOrUnderExcludedDirectory(myTreeEditor.getProject(), entry, file)) {
      return AllIcons.Modules.ExcludeRoot;
    }

    final SourceFolder[] sourceFolders = entry.getSourceFolders();
    for (SourceFolder sourceFolder : sourceFolders) {
      if (file.equals(sourceFolder.getFile())) {
        return SourceRootPresentation.getSourceRootIcon(sourceFolder);
      }
    }

    Icon icon = originalIcon;
    VirtualFile currentRoot = null;
    for (SourceFolder sourceFolder : sourceFolders) {
      final VirtualFile sourcePath = sourceFolder.getFile();
      if (sourcePath != null && VfsUtilCore.isAncestor(sourcePath, file, true)) {
        if (currentRoot != null && VfsUtilCore.isAncestor(sourcePath, currentRoot, false)) {
          continue;
        }
        Icon folderIcon = getSourceFolderIcon(sourceFolder.getRootType());
        if (folderIcon != null) {
          icon = folderIcon;
        }
        currentRoot = sourcePath;
      }
    }
    return icon;
  }

  @Nullable
  private Icon getSourceFolderIcon(JpsModuleSourceRootType<?> type) {
    for (ModuleSourceRootEditHandler<?> handler : myEditHandlers) {
      if (handler.getRootType().equals(type)) {
        return handler.getFolderUnderRootIcon();
      }
    }
    return null;
  }
}
