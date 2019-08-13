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
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;

public interface PsiFileSystemItemFilter {

  /**
   * @param item {@link PsiFile file} or {@link PsiDirectory directory}.
   * @return {@code true} if item should be showed in project view, otherwise {@code false}.
   * @see ProjectViewDirectoryHelper#getDirectoryChildren(PsiDirectory, ViewSettings, boolean, PsiFileSystemItemFilter)
   */
  boolean shouldShow(@NotNull PsiFileSystemItem item);
}
