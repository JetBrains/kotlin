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
package com.intellij.internal;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.DirectoryIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class DumpDirectoryInfoAction extends AnAction {
  public static final Logger LOG = Logger.getInstance(DumpDirectoryInfoAction.class);

  public DumpDirectoryInfoAction() {
    super("Dump Directory Info");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final DirectoryIndex index = DirectoryIndex.getInstance(project);
    if (project != null) {
      final VirtualFile root = e.getData(CommonDataKeys.VIRTUAL_FILE);
      ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
        final ContentIterator contentIterator = fileOrDir -> {
          LOG.info(fileOrDir.getPath());

          LOG.info(index.getInfoForFile(fileOrDir).toString());
          return true;
        };
        if (root != null) {
          ProjectRootManager.getInstance(project).getFileIndex().iterateContentUnderDirectory(root, contentIterator);
        } else {
          ProjectRootManager.getInstance(project).getFileIndex().iterateContent(contentIterator);
        }
      }, "Dumping directory index", true, project);
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getData(CommonDataKeys.PROJECT) != null);
  }
}
