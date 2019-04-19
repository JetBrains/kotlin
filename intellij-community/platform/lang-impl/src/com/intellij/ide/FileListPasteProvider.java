/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.ide;

import com.intellij.ide.dnd.FileCopyPasteUtil;
import com.intellij.ide.dnd.LinuxDragAndDropSupport;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class FileListPasteProvider implements PasteProvider {
  @Override
  public void performPaste(@NotNull DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final IdeView ideView = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (project == null || ideView == null) return;

    if (!FileCopyPasteUtil.isFileListFlavorAvailable()) return;

    final Transferable contents = CopyPasteManager.getInstance().getContents();
    if (contents == null) return;
    final List<File> fileList = FileCopyPasteUtil.getFileList(contents);
    if (fileList == null) return;

    if (DumbService.isDumb(project)) {
      DumbService.getInstance(project).showDumbModeNotification("Sorry, file copy/paste is not available during indexing");
      return;
    }

    final List<PsiElement> elements = new ArrayList<>();
    for (File file : fileList) {
      final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      PsiFileSystemItem item = PsiUtilCore.findFileSystemItem(project, vFile);
      if (item != null) {
        elements.add(item);
      }
    }

    if (elements.size() > 0) {
      final PsiDirectory dir = ideView.getOrChooseDirectory();
      if (dir != null) {
        final boolean move = LinuxDragAndDropSupport.isMoveOperation(contents);
        if (move) {
          new MoveFilesOrDirectoriesHandler().doMove(PsiUtilCore.toPsiElementArray(elements), dir);
        }
        else {
          new CopyFilesOrDirectoriesHandler().doCopy(PsiUtilCore.toPsiElementArray(elements), dir);
        }
      }
    }
  }

  @Override
  public boolean isPastePossible(@NotNull DataContext dataContext) {
    return true;
  }

  @Override
  public boolean isPasteEnabled(@NotNull DataContext dataContext) {
    return LangDataKeys.IDE_VIEW.getData(dataContext) != null &&
           FileCopyPasteUtil.isFileListFlavorAvailable();
  }
}
