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

package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class ContentRootMacro extends Macro {
  @Override
  public String getName() {
    return "ContentRoot";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.content.root");
  }

  @Override
  public String expand(@NotNull final DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (project == null || file == null) return null;

    final VirtualFile contentRoot = ProjectFileIndex.SERVICE.getInstance(project).getContentRootForFile(file);
    return contentRoot == null ? null : FileUtil.toSystemDependentName(contentRoot.getPath());
  }
}
