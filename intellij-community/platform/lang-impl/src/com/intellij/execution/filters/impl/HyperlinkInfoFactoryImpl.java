/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.execution.filters.impl;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.HyperlinkInfoFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class HyperlinkInfoFactoryImpl extends HyperlinkInfoFactory {

  @NotNull
  @Override
  public HyperlinkInfo createMultipleFilesHyperlinkInfo(@NotNull List<? extends VirtualFile> files,
                                                        int line, @NotNull Project project) {
    return new MultipleFilesHyperlinkInfo(files, line, project);
  }

  @NotNull
  @Override
  public HyperlinkInfo createMultipleFilesHyperlinkInfo(@NotNull List<? extends VirtualFile> files,
                                                        int line,
                                                        @NotNull Project project,
                                                        HyperlinkInfoFactory.@Nullable HyperlinkHandler action) {
    return new MultipleFilesHyperlinkInfo(files, line, project, action);
  }

  @Override
  public @NotNull HyperlinkInfo createMultiplePsiElementHyperlinkInfo(@NotNull Collection<? extends PsiElement> elements) {
    return new MultiPsiElementHyperlinkInfo(elements);
  }
}
