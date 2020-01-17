// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is an adapter for running any FileProcessingCompiler as a compiler task.
 *
 * @author Eugene Zhuravlev
 */
public class FileProcessingCompilerAdapterTask implements CompileTask {
  private static final Logger LOG = Logger.getInstance(FileProcessingCompilerAdapterTask.class);

  private final FileProcessingCompiler myCompiler;

  public FileProcessingCompilerAdapterTask(FileProcessingCompiler compiler) {
    myCompiler = compiler;
  }

  public FileProcessingCompiler getCompiler() {
    return myCompiler;
  }

  @Override
  public boolean execute(@NotNull CompileContext context) {
    final Project project = context.getProject();

    try {
      final FileProcessingCompiler.ProcessingItem[] items = myCompiler.getProcessingItems(context);
      if (items.length == 0) {
        return true;
      }

      final List<FileProcessingCompiler.ProcessingItem> toProcess = new ArrayList<>();
      final Ref<IOException> ex = new Ref<>(null);
      final FileProcessingCompilerStateCache cache = getCache(context);
      final boolean isMake = context.isMake();
      DumbService.getInstance(project).runReadActionInSmartMode(() -> {
        try {
          for (FileProcessingCompiler.ProcessingItem item : items) {
            final VirtualFile file = item.getFile();
            final String url = file.getUrl();
            if (isMake && cache.getTimestamp(url) == file.getTimeStamp()) {
              final ValidityState state = cache.getExtState(url);
              final ValidityState itemState = item.getValidityState();
              if (state != null ? state.equalsTo(itemState) : itemState == null) {
                continue;
              }
            }
            toProcess.add(item);
          }
        }
        catch (IOException e) {
          ex.set(e);
        }
      });

      IOException exception = ex.get();
      if (exception != null) {
        throw exception;
      }

      if (toProcess.isEmpty()) {
        return true;
      }

      final FileProcessingCompiler.ProcessingItem[] array = toProcess.toArray(FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY);
      final FileProcessingCompiler.ProcessingItem[] processed = myCompiler.process(context, array);
      if (processed.length == 0) {
        return true;
      }

      CompilerUtil.runInContext(context, CompilerBundle.message("progress.updating.caches"), () -> {
        final List<VirtualFile> vFiles = new ArrayList<>(processed.length);
        final List<Pair<FileProcessingCompiler.ProcessingItem, ValidityState>> toUpdate = new ArrayList<>(processed.length);
        ApplicationManager.getApplication().runReadAction(() -> {
          for (FileProcessingCompiler.ProcessingItem item : processed) {
            vFiles.add(item.getFile());
            toUpdate.add(Pair.create(item, item.getValidityState()));
          }
        });
        LocalFileSystem.getInstance().refreshFiles(vFiles);

        for (Pair<FileProcessingCompiler.ProcessingItem, ValidityState> pair : toUpdate) {
          cache.update(pair.getFirst().getFile(), pair.getSecond());
        }
      });
    }
    catch (Throwable e) {
      context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
      LOG.info(e);
    }
    return true;
  }

  private FileProcessingCompilerStateCache getCache(CompileContext context) throws IOException {
    final CompilerCacheManager cacheManager = CompilerCacheManager.getInstance(context.getProject());
    try {
      return cacheManager.getFileProcessingCompilerCache(myCompiler);
    }
    catch (IOException e) {
      cacheManager.clearCaches(context);
    }
    return cacheManager.getFileProcessingCompilerCache(myCompiler);
  }
}
