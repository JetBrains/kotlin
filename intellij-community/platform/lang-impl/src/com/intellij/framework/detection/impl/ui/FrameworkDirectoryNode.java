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
package com.intellij.framework.detection.impl.ui;

import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.util.PlatformIcons;

import javax.swing.tree.TreeNode;
import java.io.File;

/**
 * @author nik
 */
class FrameworkDirectoryNode extends DetectedFrameworkTreeNodeBase {
  private static final Logger LOG = Logger.getInstance("#com.intellij.framework.detection.impl.ui.FrameworkDirectoryNode");
  private final VirtualFile myDirectory;

  FrameworkDirectoryNode(VirtualFile directory) {
    super(directory);
    myDirectory = directory;
  }

  @Override
  public void renderNode(ColoredTreeCellRenderer renderer) {
    renderer.setIcon(PlatformIcons.FOLDER_ICON);
    renderer.append(getRelativePath());
  }

  private String getRelativePath() {
    final TreeNode parent = getParent();
    String path;
    if (parent instanceof FrameworkDirectoryNode) {
      final VirtualFile parentDir = ((FrameworkDirectoryNode)parent).myDirectory;
      path = VfsUtilCore.getRelativePath(myDirectory, parentDir, File.separatorChar);
      LOG.assertTrue(path != null, myDirectory + " is not under " + parentDir);
    }
    else {
      path = myDirectory.getPresentableUrl();
    }
    return path;
  }

  @Override
  public String getCheckedDescription() {
    return null;
  }

  @Override
  public String getUncheckedDescription() {
    return "'" + getRelativePath() + "' directory will be excluded from framework detection";
  }

  @Override
  public void disableDetection(DetectionExcludesConfiguration configuration) {
    configuration.addExcludedFile(myDirectory, null);
  }
}
