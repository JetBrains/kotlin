// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.projectView.impl.AbstractUrl;
import com.intellij.ide.util.treeView.WeighedItem;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ChangeListsScopesProvider;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.util.TreeItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author gregsh
 */
public class DefaultSearchScopeProviders {
  private DefaultSearchScopeProviders() {}

  public static class Favorites implements SearchScopeProvider {
    @Override
    public String getDisplayName() {
      return "Favorites";
    }

    @NotNull
    @Override
    public List<SearchScope> getSearchScopes(@NotNull Project project) {
      FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
      if (favoritesManager == null) return Collections.emptyList();
      List<SearchScope> result = new ArrayList<>();
      for (String favorite : favoritesManager.getAvailableFavoritesListNames()) {
        Collection<TreeItem<Pair<AbstractUrl, String>>> rootUrls = favoritesManager.getFavoritesListRootUrls(favorite);
        if (rootUrls.isEmpty()) continue;  // ignore unused root
        result.add(new GlobalSearchScope(project) {
          @NotNull
          @Override
          public String getDisplayName() {
            return "Favorite \'" + favorite + "\'";
          }

          @Override
          public boolean contains(@NotNull VirtualFile file) {
            return ReadAction.compute(() -> favoritesManager.contains(favorite, file));
          }

          @Override
          public boolean isSearchInModuleContent(@NotNull Module aModule) {
            return true;
          }

          @Override
          public boolean isSearchInLibraries() {
            return true;
          }
        });
      }
      return result;
    }
  }

  public static class ChangeLists implements SearchScopeProvider {
    @Override
    public String getDisplayName() {
      return "Local Changes";
    }

    @NotNull
    @Override
    public List<SearchScope> getSearchScopes(@NotNull Project project) {
      List<SearchScope> result = new ArrayList<>();
      List<NamedScope> changeLists = ChangeListsScopesProvider.getInstance(project).getFilteredScopes();
      if (!changeLists.isEmpty()) {
        for (NamedScope changeListScope : changeLists) {
          result.add(wrapNamedScope(project, changeListScope));
        }
      }
      return result;
    }
  }

  public static class CustomNamed implements SearchScopeProvider {
    @Override
    public String getDisplayName() {
      return "Other";
    }

    @NotNull
    @Override
    public List<SearchScope> getSearchScopes(@NotNull Project project) {
      List<SearchScope> result = new ArrayList<>();
      NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(project);
      for (NamedScopesHolder holder : holders) {
        NamedScope[] scopes = holder.getEditableScopes();  // predefined scopes already included
        for (NamedScope scope : scopes) {
          result.add(wrapNamedScope(project, scope));
        }
      }
      return result;
    }
  }

  @NotNull
  private static GlobalSearchScope wrapNamedScope(@NotNull Project project, @NotNull NamedScope namedScope) {
    GlobalSearchScope scope = GlobalSearchScopesCore.filterScope(project, namedScope);
    return namedScope instanceof WeighedItem ? new MyWeightedScope(scope, ((WeighedItem)namedScope).getWeight()) : scope;
  }

  private static class MyWeightedScope extends DelegatingGlobalSearchScope implements WeighedItem {
    final int weight;

    MyWeightedScope(@NotNull GlobalSearchScope scope, int weight) {
      super(scope);
      this.weight = weight;
    }

    @Override
    public int getWeight() {
      return weight;
    }
  }
}
