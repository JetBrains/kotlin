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
package com.intellij.ide.util.gotoByName;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DefaultFileNavigationContributor implements ChooseByNameContributorEx, DumbAware {

  @Override
  @NotNull
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    THashSet<String> names = new THashSet<>(1000);
    IdFilter filter = IdFilter.getProjectIdFilter(project, includeNonProjectItems);
    processNames(s -> {
      names.add(s);
      return true;
    }, FindSymbolParameters.searchScopeFor(project, includeNonProjectItems), filter);
    if (IdFilter.LOG.isDebugEnabled()) {
      IdFilter.LOG.debug("All names retrieved2:" + names.size());
    }
    return ArrayUtil.toStringArray(names);
  }

  @Override
  @NotNull
  public NavigationItem[] getItemsByName(String name, final String pattern, Project project, boolean includeNonProjectItems) {
    List<NavigationItem> result = new ArrayList<>();
    Processor<NavigationItem> processor = Processors.cancelableCollectProcessor(result);
    processElementsWithName(name, processor, FindSymbolParameters.wrap(pattern, project, includeNonProjectItems));

    return result.isEmpty() ? NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY : result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
  }

  @Override
  public void processNames(@NotNull final Processor<String> processor, @NotNull GlobalSearchScope scope, IdFilter filter) {
    long started = System.currentTimeMillis();
    FilenameIndex.processAllFileNames(processor, scope, filter);
    if (IdFilter.LOG.isDebugEnabled()) {
      IdFilter.LOG.debug("All names retrieved:" + (System.currentTimeMillis() - started));
    }
  }

  @Override
  public void processElementsWithName(@NotNull String name,
                                      @NotNull final Processor<NavigationItem> _processor,
                                      @NotNull FindSymbolParameters parameters) {
    final boolean globalSearch = parameters.getSearchScope().isSearchInLibraries();
    final Processor<PsiFileSystemItem> processor = item -> {
      if (!globalSearch && ProjectUtil.isProjectOrWorkspaceFile(item.getVirtualFile())) {
        return true;
      }
      return _processor.process(item);
    };

    boolean directoriesOnly = isDirectoryOnlyPattern(parameters);
    if (!directoriesOnly) {
      FilenameIndex.processFilesByName(
        name, false, processor, parameters.getSearchScope(), parameters.getProject(), parameters.getIdFilter()
      );
    }

    if (directoriesOnly || Registry.is("ide.goto.file.include.directories")) {
      FilenameIndex.processFilesByName(
        name, true, processor, parameters.getSearchScope(), parameters.getProject(), parameters.getIdFilter()
      );
    }
  }

  private static boolean isDirectoryOnlyPattern(@NotNull FindSymbolParameters parameters) {
    String completePattern = parameters.getCompletePattern();
    return completePattern.endsWith("/") || completePattern.endsWith("\\");
  }
}
