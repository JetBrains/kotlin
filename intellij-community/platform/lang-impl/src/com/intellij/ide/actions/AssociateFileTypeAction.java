/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.openapi.fileTypes.ex.FileTypeChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AssociateFileTypeAction extends AnAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
    FileTypeChooser.associateFileType(file.getName());
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    Project project = e.getProject();
    boolean haveSmthToDo;
    if (project == null || file == null || file.isDirectory()) {
      haveSmthToDo = false;
    }
    else {
      // the action should also be available for files which have been auto-detected as text or as a particular language (IDEA-79574)
      haveSmthToDo = FileTypeManager.getInstance().getFileTypeByFileName(file.getNameSequence()) == FileTypes.UNKNOWN &&
                     !(file.getFileSystem() instanceof NonPhysicalFileSystem) &&
                     !ScratchRootType.getInstance().containsFile(file);
      haveSmthToDo |= ActionPlaces.isMainMenuOrActionSearch(e.getPlace());
    }
    presentation.setVisible(haveSmthToDo || ActionPlaces.isMainMenuOrActionSearch(e.getPlace()));
    presentation.setEnabled(haveSmthToDo);
  }

}
