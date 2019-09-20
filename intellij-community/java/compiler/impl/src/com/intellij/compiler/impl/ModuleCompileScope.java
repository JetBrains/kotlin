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

/*
 * @author: Eugene Zhuravlev
 */
package com.intellij.compiler.impl;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.CommonProcessors;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModuleCompileScope extends FileIndexCompileScope {
  private final Project myProject;
  private final Set<Module> myScopeModules;
  private final Module[] myModules;
  private final Collection<String> myIncludedUnloadedModules;

  public ModuleCompileScope(final Module module, boolean includeDependentModules) {
    this(module.getProject(), Collections.singleton(module), Collections.emptyList(), includeDependentModules, false);
  }

  public ModuleCompileScope(Project project, final Module[] modules, boolean includeDependentModules) {
    this(project, modules, includeDependentModules, false);
  }

  public ModuleCompileScope(Project project, final Module[] modules, boolean includeDependentModules, boolean includeRuntimeDependencies) {
    this(project, Arrays.asList(modules), Collections.emptyList(), includeDependentModules, includeRuntimeDependencies);
  }

  public ModuleCompileScope(Project project, final Collection<? extends Module> modules, Collection<String> includedUnloadedModules, boolean includeDependentModules, boolean includeRuntimeDeps) {
    myProject = project;
    myIncludedUnloadedModules = includedUnloadedModules;
    myScopeModules = new HashSet<>();
    for (Module module : modules) {
      if (module == null) {
        continue; // prevent NPE
      }
      if (includeDependentModules) {
        OrderEnumerator enumerator = ModuleRootManager.getInstance(module).orderEntries().recursively();
        if (!includeRuntimeDeps) {
          enumerator = enumerator.compileOnly();
        }
        enumerator.forEachModule(new CommonProcessors.CollectProcessor<>(myScopeModules));
      }
      else {
        myScopeModules.add(module);
      }
    }
    myModules = ModuleManager.getInstance(myProject).getModules();
  }

  @Override
  @NotNull
  public Module[] getAffectedModules() {
    return myScopeModules.toArray(Module.EMPTY_ARRAY);
  }

  @NotNull
  @Override
  public Collection<String> getAffectedUnloadedModules() {
    return Collections.unmodifiableCollection(myIncludedUnloadedModules);
  }

  @Override
  protected FileIndex[] getFileIndices() {
    final FileIndex[] indices = new FileIndex[myScopeModules.size()];
    int idx = 0;
    for (final Module module : myScopeModules) {
      indices[idx++] = ModuleRootManager.getInstance(module).getFileIndex();
    }
    return indices;
  }

  @Override
  public boolean belongs(@NotNull final String url) {
    if (myScopeModules.isEmpty() && myIncludedUnloadedModules.isEmpty()) {
      return false; // optimization
    }
    Module candidateModule = null;
    int maxUrlLength = 0;
    final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    for (final Module module : myModules) {
      final String[] contentRootUrls = getModuleContentUrls(module);
      for (final String contentRootUrl : contentRootUrls) {
        if (contentRootUrl.length() < maxUrlLength) {
          continue;
        }
        if (!isUrlUnderRoot(url, contentRootUrl)) {
          continue;
        }
        if (contentRootUrl.length() == maxUrlLength) {
          if (candidateModule == null) {
            candidateModule = module;
          }
          else {
            // the same content root exists in several modules
            if (!candidateModule.equals(module)) {
              candidateModule = ReadAction.compute(() -> {
                final VirtualFile contentRootFile = VirtualFileManager.getInstance().findFileByUrl(contentRootUrl);
                if (contentRootFile != null) {
                  return projectFileIndex.getModuleForFile(contentRootFile);
                }
                return null;
              });
            }
          }
        }
        else {
          maxUrlLength = contentRootUrl.length();
          candidateModule = module;
        }
      }
    }

    if (candidateModule != null && myScopeModules.contains(candidateModule)) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(candidateModule);
      final String[] excludeRootUrls = moduleRootManager.getExcludeRootUrls();
      for (String excludeRootUrl : excludeRootUrls) {
        if (isUrlUnderRoot(url, excludeRootUrl)) {
          return false;
        }
      }
      final String[] sourceRootUrls = moduleRootManager.getSourceRootUrls();
      for (String sourceRootUrl : sourceRootUrls) {
        if (isUrlUnderRoot(url, sourceRootUrl)) {
          return true;
        }
      }
    }

    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    for (String unloadedModule : myIncludedUnloadedModules) {
      UnloadedModuleDescription moduleDescription = moduleManager.getUnloadedModuleDescription(unloadedModule);
      if (moduleDescription != null) {
        for (VirtualFilePointer pointer : moduleDescription.getContentRoots()) {
          if (isUrlUnderRoot(url, pointer.getUrl())) {
            return true;
          }
        }
      }
    }

    return false;
  }

  private static boolean isUrlUnderRoot(final String url, final String root) {
    return (url.length() > root.length()) && url.charAt(root.length()) == '/' && FileUtil.startsWith(url, root);
  }

  private final Map<Module, String[]> myContentUrlsCache = new HashMap<>();

  private String[] getModuleContentUrls(final Module module) {
    String[] contentRootUrls = myContentUrlsCache.get(module);
    if (contentRootUrls == null) {
      contentRootUrls = ModuleRootManager.getInstance(module).getContentRootUrls();
      myContentUrlsCache.put(module, contentRootUrls);
    }
    return contentRootUrls;
  }

}
