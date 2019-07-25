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
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.externalSystem.ExternalSystemAutoImportAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link ExternalSystemAutoImportAware} implementation which caches positive answers, i.e. mappings between file paths and
 * corresponding root external project path.
 * 
 * @author Denis Zhdanov
 */
public class CachingExternalSystemAutoImportAware implements ExternalSystemAutoImportAware {

  @NotNull private final ConcurrentMap<String/* file path */, String/* root external project path */> myCache
    = ContainerUtil.newConcurrentMap();
  
  @NotNull private final ExternalSystemAutoImportAware myDelegate;

  public CachingExternalSystemAutoImportAware(@NotNull ExternalSystemAutoImportAware delegate) {
    myDelegate = delegate;
  }

  @Nullable
  @Override
  public String getAffectedExternalProjectPath(@NotNull String changedFileOrDirPath, @NotNull Project project) {
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
}
