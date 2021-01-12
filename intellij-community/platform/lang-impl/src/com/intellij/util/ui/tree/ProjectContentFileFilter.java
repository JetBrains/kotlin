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
package com.intellij.util.ui.tree;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.annotations.NotNull;

public class ProjectContentFileFilter implements VirtualFileFilter {
  private final Project project;
  private final VirtualFileFilter filter;

  private ProjectFileIndex fileIndex;

  public ProjectContentFileFilter(@NotNull Project project, @NotNull VirtualFileFilter filter) {
    this.project = project;
    this.filter = filter;
  }

  @Override
  public boolean accept(@NotNull VirtualFile file) {
    if (!filter.accept(file)) {
      return false;
    }

    if (fileIndex == null) {
      fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    }
    return fileIndex.isInContent(file);
  }
}
