// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.scope.TestsScope;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class TestScopeProvider extends CustomScopesProviderEx {
  public static TestScopeProvider getInstance(Project project) {
    return CUSTOM_SCOPES_PROVIDER.findExtension(TestScopeProvider.class, project);
  }

  @Override
  @NotNull
  public List<NamedScope> getCustomScopes() {
    return Collections.singletonList(TestsScope.INSTANCE);
  }
}
