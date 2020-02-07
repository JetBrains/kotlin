// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class FileBasedIndexScanRunnableCollector {
  public static FileBasedIndexScanRunnableCollector getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, FileBasedIndexScanRunnableCollector.class);
  }

  // Returns true if file should be indexed
  public abstract boolean shouldCollect(@NotNull final VirtualFile file);

  // Collect all roots for indexing
  public abstract List<Runnable> collectScanRootRunnables(@NotNull final ContentIterator processor, final ProgressIndicator indicator);
}