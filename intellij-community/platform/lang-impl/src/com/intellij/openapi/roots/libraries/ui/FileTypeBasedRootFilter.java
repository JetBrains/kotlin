/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots.libraries.ui;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link RootDetector} which detects a root by presence of files of some specified type under it
 *
 * @author nik
 * @deprecated use {@link DescendentBasedRootFilter#createFileTypeBasedFilter(OrderRootType, boolean, FileType, String)} instead
 */
@Deprecated
public class FileTypeBasedRootFilter extends RootFilter {
  private final FileType myFileType;
  private final RootFilter myDelegate;

  public FileTypeBasedRootFilter(OrderRootType rootType, boolean jarDirectory, @NotNull FileType fileType, String presentableRootTypeName) {
    super(rootType, jarDirectory, presentableRootTypeName);
    myFileType = fileType;
    myDelegate = new DescendentBasedRootFilter(rootType, jarDirectory, presentableRootTypeName, this::isFileAccepted);
  }

  @Override
  public boolean isAccepted(@NotNull VirtualFile rootCandidate, @NotNull ProgressIndicator progressIndicator) {
    return myDelegate.isAccepted(rootCandidate, progressIndicator);
  }

  protected boolean isFileAccepted(VirtualFile virtualFile) {
    return virtualFile.getFileType().equals(myFileType);
  }
}
