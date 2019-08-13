// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.LocalChangeList;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChangeListsScopesProvider extends CustomScopesProviderEx {
  @NotNull
  private final Project myProject;

  public static ChangeListsScopesProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtension(ChangeListsScopesProvider.class, project);
  }

  public ChangeListsScopesProvider(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  @Override
  public List<NamedScope> getCustomScopes() {

    if (myProject.isDefault() || !ProjectLevelVcsManager.getInstance(myProject).hasAnyMappings()) return Collections.emptyList();
    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);

    final List<NamedScope> result = new ArrayList<>();
    result.add(new ChangeListScope(changeListManager));
    List<LocalChangeList> changeLists = changeListManager.getChangeListsCopy();
    boolean skipSingleDefaultCL = Registry.is("vcs.skip.single.default.changelist") &&
                                  changeLists.size() == 1 && changeLists.get(0).isBlank();
    if (!skipSingleDefaultCL) {
      for (ChangeList list : changeLists) {
        result.add(new ChangeListScope(changeListManager, list.getName()));
      }
    }
    return result;
  }

  @Override
  public NamedScope getCustomScope(@NotNull String name) {
    if (myProject.isDefault()) return null;
    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
    if (ChangeListScope.NAME.equals(name)) {
      return new ChangeListScope(changeListManager);
    }
    final LocalChangeList changeList = changeListManager.findChangeList(name);
    if (changeList != null) {
      return new ChangeListScope(changeListManager, changeList.getName());
    }
    return null;
  }

  @Override
  public boolean isVetoed(NamedScope scope, ScopePlace place) {
    if (place == ScopePlace.SETTING) {
      if (myProject.isDefault()) return false;
      final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
      return changeListManager.findChangeList(scope.getName()) != null;
    }
    return false;
  }
}
