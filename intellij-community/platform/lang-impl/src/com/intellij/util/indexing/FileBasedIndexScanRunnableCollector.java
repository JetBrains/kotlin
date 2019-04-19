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
