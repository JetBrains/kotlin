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

import com.intellij.openapi.editor.HectorComponentPanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.include.FileIncludeManager;
import com.intellij.ui.ComboboxWithBrowseButton;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author mike
 */
public class FileIncludeContextHectorPanel extends HectorComponentPanel {
  private ComboboxWithBrowseButton myContextFile;
  private JPanel myPanel;
  private final PsiFile myFile;
  private final FileIncludeManager myIncludeManager;

  public FileIncludeContextHectorPanel(final PsiFile file, final FileIncludeManager includeManager) {
    myFile = file;
    myIncludeManager = includeManager;

    reset();
  }


  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
  }

  @Override
  public void reset() {
    final JComboBox comboBox = myContextFile.getComboBox();

    comboBox.setRenderer(new MyListCellRenderer(comboBox));
    final VirtualFile[] includingFiles = myIncludeManager.getIncludingFiles(myFile.getVirtualFile(), false);
    comboBox.setModel(new DefaultComboBoxModel(includingFiles));
    myContextFile.setTextFieldPreferredWidth(30);
  }

  private class MyListCellRenderer extends DefaultListCellRenderer {
    private final JComboBox myComboBox;
    private int myMaxWidth;

    MyListCellRenderer(final JComboBox comboBox) {
      myComboBox = comboBox;
      myMaxWidth = comboBox.getPreferredSize().width;
    }

    @Override
    public Component getListCellRendererComponent(final JList list,
                                                  final Object value,
                                                  final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {

      final Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      String path = getPath(value);
      if (path != null) {
        final int max = index == -1 ? myComboBox.getWidth() - myContextFile.getButton().getWidth() : myComboBox.getWidth() * 3;
        path = trimPath(path, myComboBox, "/", max);
        setText(path);
      }
      return rendererComponent;
    }

    @Nullable
    protected String getPath(final Object value) {
      final VirtualFile file = (VirtualFile)value;
      final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myFile.getProject()).getFileIndex();
      if (file != null) {
        VirtualFile root = fileIndex.getSourceRootForFile(file);
        if (root == null) {
          root = fileIndex.getContentRootForFile(file);
        }
        if (root != null) {
          return VfsUtilCore.getRelativePath(file, root, '/');
        }
      }
      return null;
    }

    private String trimPath(String path, Component component, String separator, int length) {

      final FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
      final int maxWidth = fontMetrics.stringWidth(path);
      if (maxWidth <= length) {
        myMaxWidth = Math.max(maxWidth, myMaxWidth);
        return path;
      }
      final StringBuilder result = new StringBuilder(path);
      if (path.startsWith(separator)) {
        result.delete(0, 1);
      }
      final String[] strings = result.toString().split(separator);
      result.replace(0, strings[0].length(), "...");
      for (int i = 1; i < strings.length; i++) {
        final String clipped = result.toString();
        final int width = fontMetrics.stringWidth(clipped);
        if (width <= length) {
          myMaxWidth = Math.max(width, myMaxWidth);
          return clipped;
        }
        result.delete(4, 5 + strings[i].length());
      }
      return result.toString();
    }

  }

}
