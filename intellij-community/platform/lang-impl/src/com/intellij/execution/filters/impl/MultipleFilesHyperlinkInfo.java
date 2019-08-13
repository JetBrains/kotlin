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
package com.intellij.execution.filters.impl;

import com.intellij.execution.filters.FileHyperlinkInfo;
import com.intellij.execution.filters.HyperlinkInfoBase;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.ide.util.gotoByName.GotoFileCellRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
* @author nik
*/
class MultipleFilesHyperlinkInfo extends HyperlinkInfoBase implements FileHyperlinkInfo {
  private final List<? extends VirtualFile> myVirtualFiles;
  private final int myLineNumber;
  private final Project myProject;
  private final ToIntFunction<? super PsiFile> myColumnFinder;

  MultipleFilesHyperlinkInfo(@NotNull List<? extends VirtualFile> virtualFiles, int lineNumber, @NotNull Project project) {
    this(virtualFiles, lineNumber, project, null);
  }

  MultipleFilesHyperlinkInfo(@NotNull List<? extends VirtualFile> virtualFiles,
                             int lineNumber,
                             @NotNull Project project,
                             @Nullable ToIntFunction<? super PsiFile> columnFinder) {
    myVirtualFiles = virtualFiles;
    myLineNumber = lineNumber;
    myProject = project;
    myColumnFinder = columnFinder == null ? f -> 0 : columnFinder;
  }

  @Override
  public void navigate(@NotNull final Project project, @Nullable RelativePoint hyperlinkLocationPoint) {
    List<PsiFile> currentFiles = new ArrayList<>();

    ApplicationManager.getApplication().runReadAction(() -> {
      for (VirtualFile file : myVirtualFiles) {
        if (!file.isValid()) continue;

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
          PsiElement navigationElement = psiFile.getNavigationElement(); // Sources may be downloaded.
          if (navigationElement instanceof PsiFile) {
            currentFiles.add((PsiFile)navigationElement);
            continue;
          }
          currentFiles.add(psiFile);
        }
      }
    });

    if (currentFiles.isEmpty()) return;

    if (currentFiles.size() == 1) {
      PsiFile file = currentFiles.get(0);
      new OpenFileHyperlinkInfo(myProject, file.getVirtualFile(), myLineNumber, myColumnFinder.applyAsInt(file)).navigate(project);
    }
    else {
      JFrame frame = WindowManager.getInstance().getFrame(project);
      int width = frame != null ? frame.getSize().width : 200;
      JBPopup popup = JBPopupFactory.getInstance()
        .createPopupChooserBuilder(currentFiles)
        .setRenderer(new GotoFileCellRenderer(width))
        .setTitle("Choose Target File")
        .setItemChosenCallback((selectedValue) -> {
          VirtualFile file = selectedValue.getVirtualFile();
          new OpenFileHyperlinkInfo(myProject, file, myLineNumber, myColumnFinder.applyAsInt(selectedValue)).navigate(project);
        })
        .createPopup();
      if (hyperlinkLocationPoint != null) {
        popup.show(hyperlinkLocationPoint);
      }
      else {
        popup.showInFocusCenter();
      }
    }
  }

  @Nullable
  @Override
  public OpenFileDescriptor getDescriptor() {
    VirtualFile file = getPreferredFile();
    return file != null ? new OpenFileDescriptor(myProject, file, myLineNumber, 0) : null;
  }

  @Nullable
  private VirtualFile getPreferredFile() {
    return ContainerUtil.getFirstItem(myVirtualFiles);
  }
}
