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

/*
 * @author max
 */
package com.intellij.ide;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PsiIconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FileIconPatcherImpl implements FileIconProvider {
  @Override
  @Nullable
  public Icon getIcon(@NotNull final VirtualFile file, final int flags, final Project project) {
    PsiFileSystemItem psiFile = PsiUtilCore.findFileSystemItem(project, file);
    return psiFile == null ? null : PsiIconUtil.getProvidersIcon(psiFile, flags);
  }
}