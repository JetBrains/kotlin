// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link ExternalSystemAutoImportAware} implementation which caches positive answers, i.e. mappings between file paths and
 * corresponding root external project path.
 *
 * @author Denis Zhdanov
 */
public class CachingExternalSystemAutoImportAware implements ExternalSystemAutoImportAware {

  private final @NotNull ConcurrentMap<String/* file path */, String/* root external project path */> myCache
    = new ConcurrentHashMap<>();

  private final @NotNull ExternalSystemAutoImportAware myDelegate;

  public CachingExternalSystemAutoImportAware(@NotNull ExternalSystemAutoImportAware delegate) {
    myDelegate = delegate;
  }

  @Override
  public @Nullable String getAffectedExternalProjectPath(@NotNull String changedFileOrDirPath, @NotNull Project project) {
    String cached = myCache.get(changedFileOrDirPath);
    if (cached != null) {
      return cached;
    }
    String result = myDelegate.getAffectedExternalProjectPath(changedFileOrDirPath, project);
    if (result != null) {
      myCache.put(changedFileOrDirPath, result);
    }
    return result;
  }

  @Override
  public List<File> getAffectedExternalProjectFiles(String projectPath, @NotNull Project project) {
    return myDelegate.getAffectedExternalProjectFiles(projectPath, project);
  }

  @Override
  public boolean isApplicable(@Nullable ProjectResolverPolicy resolverPolicy) {
    return myDelegate.isApplicable(resolverPolicy);
  }
}
