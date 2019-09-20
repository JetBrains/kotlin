// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies;

import com.intellij.ide.scratch.ScratchesNamedScope;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.NonProjectFilesScope;
import com.intellij.psi.search.scope.ProblemsScope;
import com.intellij.psi.search.scope.ProjectFilesScope;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class DefaultScopesProvider extends CustomScopesProviderEx {
  private final Project myProject;
  private final List<NamedScope> myScopes;

  public static DefaultScopesProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtension(DefaultScopesProvider.class, project);
  }

  public DefaultScopesProvider(@NotNull Project project) {
    myProject = project;
    myScopes = Arrays.asList(ProjectFilesScope.INSTANCE,
                             getAllScope(),
                             NonProjectFilesScope.INSTANCE,
                             new ScratchesNamedScope());
  }

  @Override
  @NotNull
  public List<NamedScope> getCustomScopes() {
    return myScopes;
  }

  /**
   * @deprecated use {@link ProblemsScope#INSTANCE} instead
   */
  @Deprecated
  @NotNull
  public NamedScope getProblemsScope() {
    return ProblemsScope.INSTANCE;
  }
}
