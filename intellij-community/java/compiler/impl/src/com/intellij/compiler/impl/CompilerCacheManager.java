// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 */
@Service
public final class CompilerCacheManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(CompilerCacheManager.class);
  private final Map<Compiler, Object> myCompilerToCacheMap = new HashMap<>();
  private final List<Disposable> myCacheDisposables = new ArrayList<>();
  private final File myCachesRoot;

  public CompilerCacheManager(@NotNull Project project) {
    myCachesRoot = CompilerPaths.getCacheStoreDirectory(project);
  }

  public static CompilerCacheManager getInstance(Project project) {
    return project.getService(CompilerCacheManager.class);
  }

  @Override
  public void dispose() {
    flushCaches();
  }

  private File getCompilerRootDir(final Compiler compiler) {
    final File dir = new File(myCachesRoot, getCompilerIdString(compiler));
    dir.mkdirs();
    return dir;
  }

  synchronized FileProcessingCompilerStateCache getFileProcessingCompilerCache(final FileProcessingCompiler compiler) throws IOException {
    Object cache = myCompilerToCacheMap.get(compiler);
    if (cache == null) {
      final File compilerRootDir = getCompilerRootDir(compiler);
      final FileProcessingCompilerStateCache stateCache = new FileProcessingCompilerStateCache(compilerRootDir, compiler);
      myCompilerToCacheMap.put(compiler, stateCache);
      myCacheDisposables.add(() -> {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Closing cache for compiler " + compiler.getDescription() + "; cache root dir: " + compilerRootDir);
        }
        stateCache.close();
      });
      cache = stateCache;
    }
    else {
      LOG.assertTrue(cache instanceof FileProcessingCompilerStateCache);
    }
    return (FileProcessingCompilerStateCache)cache;
  }

  public static String getCompilerIdString(Compiler compiler) {
    @NonNls String description = compiler instanceof Validator ? ((Validator)compiler).getId() : compiler.getDescription();
    return StringUtil.toLowerCase(description.replaceAll("\\s+", "_").replaceAll("[\\.\\?]", "_"));
  }

  synchronized void flushCaches() {
    for (Disposable disposable : myCacheDisposables) {
      try {
        Disposer.dispose(disposable);
      }
      catch (Throwable e) {
        LOG.info(e);
      }
    }
    myCacheDisposables.clear();
    myCompilerToCacheMap.clear();
  }

  public void clearCaches(final CompileContext context) {
    flushCaches();
    final File[] children = myCachesRoot.listFiles();
    if (children != null) {
      for (final File child : children) {
        final boolean deleteOk = FileUtil.delete(child);
        if (!deleteOk) {
          context.addMessage(CompilerMessageCategory.ERROR, JavaCompilerBundle.message("compiler.error.failed.to.delete", child.getPath()), null, -1, -1);
        }
      }
    }
  }
}
