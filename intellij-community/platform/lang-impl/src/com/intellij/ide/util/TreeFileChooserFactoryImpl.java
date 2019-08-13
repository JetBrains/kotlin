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

package com.intellij.ide.util;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
public class TreeFileChooserFactoryImpl extends TreeFileChooserFactory {
  private final Project myProject;

  public TreeFileChooserFactoryImpl(Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  public TreeFileChooser createFileChooser(@NotNull String title, @Nullable PsiFile initialFile, @Nullable FileType fileType, @Nullable TreeFileChooser.PsiFileFilter filter) {
    return new TreeFileChooserDialog(myProject, title, initialFile, fileType, filter, false, false);
  }

  @Override
  @NotNull
  public TreeFileChooser createFileChooser(@NotNull String title, @Nullable PsiFile initialFile, @Nullable FileType fileType, @Nullable TreeFileChooser.PsiFileFilter filter,
                                           boolean disableStructureProviders) {
    return new TreeFileChooserDialog(myProject, title, initialFile, fileType, filter, disableStructureProviders, false);
  }

  @Override
  @NotNull
  public TreeFileChooser createFileChooser(@NotNull String title, @Nullable PsiFile initialFile, @Nullable FileType fileType, @Nullable TreeFileChooser.PsiFileFilter filter,
                                           boolean disableStructureProviders,
                                           boolean showLibraryContents) {
    return new TreeFileChooserDialog(myProject, title, initialFile, fileType, filter, disableStructureProviders, showLibraryContents);
  }
}
