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
package com.intellij.packaging.impl.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class FileCopyPresentation extends PackagingElementPresentation {
  private final String mySourcePath;
  private final String myOutputFileName;
  private final VirtualFile myFile;

  public FileCopyPresentation(String filePath, String outputFileName) {
    myOutputFileName = outputFileName;

    String parentPath;
    myFile = LocalFileSystem.getInstance().findFileByPath(filePath);
    if (myFile != null) {
      final VirtualFile parent = myFile.getParent();
      parentPath = parent != null ? FileUtil.toSystemDependentName(parent.getPath()) : "";
    }
    else {
      parentPath = FileUtil.toSystemDependentName(PathUtil.getParentPath(filePath));
    }

    String sourceFileName = PathUtil.getFileName(filePath);
    if (!sourceFileName.equals(myOutputFileName)) {
      mySourcePath = parentPath + "/" + sourceFileName;
    }
    else {
      mySourcePath = parentPath;
    }
  }

  @Override
  public String getPresentableName() {
    return myOutputFileName;
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    if (myFile != null && !myFile.isDirectory()) {
      presentationData.setIcon(VirtualFilePresentation.getIcon(myFile));
      presentationData.addText(myOutputFileName, mainAttributes);
      presentationData.addText(" (" + mySourcePath + ")", commentAttributes);
    }
    else {
      presentationData.setIcon(AllIcons.FileTypes.Text);
      presentationData.addText(myOutputFileName, SimpleTextAttributes.ERROR_ATTRIBUTES);
      final VirtualFile parentFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(mySourcePath));
      presentationData.addText("(" + mySourcePath + ")",
                      parentFile != null ? commentAttributes : SimpleTextAttributes.ERROR_ATTRIBUTES);
    }
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.FILE_COPY;
  }
}
