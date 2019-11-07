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

import com.intellij.framework.detection.DetectedFrameworkDescription;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.framework.detection.FrameworkDetectionContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

/**
 * @author nik
 */
class DetectedFrameworkNode extends DetectedFrameworkTreeNodeBase {
  private static final Logger LOG = Logger.getInstance(DetectedFrameworkNode.class);
  private final DetectedFrameworkDescription myDescription;
  private final FrameworkDetectionContext myContext;

  DetectedFrameworkNode(DetectedFrameworkDescription description, FrameworkDetectionContext context) {
    super(description);
    myDescription = description;
    myContext = context;
  }

  @Override
  public void renderNode(ColoredTreeCellRenderer renderer) {
    renderer.setIcon(myDescription.getDetector().getFrameworkType().getIcon());
    final Collection<? extends VirtualFile> files = myDescription.getRelatedFiles();
    final VirtualFile firstFile = ContainerUtil.getFirstItem(files);
    LOG.assertTrue(firstFile != null);
    if (files.size() == 1) {
      renderer.append(firstFile.getName());
      appendDirectoryPath(renderer, firstFile.getParent());
    }
    else {
      String commonName = firstFile.getName();
      VirtualFile commonParent = firstFile.getParent();
      for (VirtualFile file : files) {
        if (commonName != null && !commonName.equals(file.getName())) {
          commonName = null;
        }
        if (commonParent != null && !commonParent.equals(file.getParent())) {
          commonParent = null;
        }
      }
      renderer.append(files.size() + " " + (commonName != null ? commonName : firstFile.getFileType().getDefaultExtension()) + " files");
      if (commonParent != null) {
        appendDirectoryPath(renderer, commonParent);
      }
    }
  }

  @Override
  public String getCheckedDescription() {
    return myDescription.getSetupText();
  }

  @Override
  public String getUncheckedDescription() {
    return null;
  }

  @Override
  public void disableDetection(DetectionExcludesConfiguration configuration) {
    for (VirtualFile file : myDescription.getRelatedFiles()) {
      configuration.addExcludedFile(file, myDescription.getDetector().getFrameworkType());
    }
  }

  private void appendDirectoryPath(ColoredTreeCellRenderer renderer, final VirtualFile dir) {
    final String path = getRelativePath(dir);
    renderer.append(" (" + (path.isEmpty() ? "/" : path) + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  @NotNull
  private String getRelativePath(@NotNull VirtualFile file) {
    final VirtualFile dir = myContext.getBaseDir();
    if (dir != null) {
      final String path = VfsUtilCore.getRelativePath(dir, file, File.separatorChar);
      if (path != null) {
        return path;
      }
    }
    return file.getPresentableUrl();
  }
}
