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

package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

public class FileRelativeDirMacro extends Macro {
  @Override
  public String getName() {
    return "FileRelativeDir";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.file.directory.relative");
  }

  @Override
  public String expand(DataContext dataContext) {
    final VirtualFile baseDir = PlatformDataKeys.PROJECT_FILE_DIRECTORY.getData(dataContext);
    if (baseDir == null) {
      return null;
    }

    VirtualFile dir = getVirtualDirOrParent(dataContext);
    if (dir == null) return null;
    return FileUtil.getRelativePath(VfsUtil.virtualToIoFile(baseDir), VfsUtil.virtualToIoFile(dir));
  }
}
