// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.contentQueue;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.util.io.FileTooBigException;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CurrentProjectHintedCachedFileContentLoader implements CachedFileContentLoader {
  private final Project myProject;

  public CurrentProjectHintedCachedFileContentLoader(Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  public CachedFileContent loadContent(@NotNull VirtualFile file) throws FailedToLoadContentException, TooLargeContentException {
    CachedFileContent content = new CachedFileContent(file);
    if (file.isDirectory() || !file.isValid() || file.is(VFileProperty.SPECIAL) || VfsUtilCore.isBrokenLink(file)) {
      content.setEmptyContent();
      return content;
    }

    // Reads the content bytes and caches them. Hint at the current project to avoid expensive read action in ProjectLocator.
    try {
      ProjectLocator.computeWithPreferredProject(file, myProject, () -> content.getBytes());
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (FileTooBigException e) {
      throw new TooLargeContentException(file);
    }
    catch (Throwable e) {
      throw new FailedToLoadContentException(file, e);
    }
    return content;
  }
}
