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

package com.intellij.openapi.fileChooser.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public final class GotoModuleDirectory extends FileChooserAction {
  @Override
  protected void actionPerformed(final FileSystemTree fileSystemTree, final AnActionEvent e) {
    final VirtualFile moduleDir = getModuleDir(e);
    if (moduleDir != null) {
      fileSystemTree.select(moduleDir, () -> fileSystemTree.expand(moduleDir, null));
    }
  }

  @Override
  protected void update(final FileSystemTree fileSystemTree, final AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final VirtualFile moduleDir = getModuleDir(e);
    presentation.setEnabled(moduleDir != null && fileSystemTree.isUnderRoots(moduleDir));
  }

  @Nullable
  private static VirtualFile getModuleDir(final AnActionEvent e) {
    Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    if (module == null) {
      module = e.getData(LangDataKeys.MODULE);
    }

    if (module != null && !module.isDisposed()) {
      final VirtualFile moduleFile = module.getModuleFile();
      if (moduleFile != null && moduleFile.isValid()) {
        final VirtualFile moduleDir = moduleFile.getParent();
        if (moduleDir != null && moduleDir.isValid()) {
          return moduleDir;
        }
      }
    }

    return null;
  }
}
