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

package com.intellij.psi.impl.source.resolve.reference.impl.providers;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Dmitry Avdeev
 */
public class JarFileReferenceHelper extends FileReferenceHelper {

  @Override
  public PsiFileSystemItem getPsiFileSystemItem(Project project, @NotNull VirtualFile file) {
    return null;
  }

  @Override
  @NotNull
  public Collection<PsiFileSystemItem> getRoots(@NotNull Module module) {
    return PsiFileReferenceHelper.getContextsForModule(module, "", null);
  }

  @Override
  @NotNull
  public Collection<PsiFileSystemItem> getContexts(Project project, @NotNull VirtualFile file) {
    return Collections.emptyList();
  }

  @Override
  public boolean isMine(Project project, @NotNull VirtualFile file) {
    return ProjectRootManager.getInstance(project).getFileIndex().isInLibraryClasses(file);
  }
}
