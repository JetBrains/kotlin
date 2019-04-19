// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scopeView;

import com.intellij.ide.scratch.ScratchesNamedScope;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.NonProjectFilesScope;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.psi.search.scope.packageSet.PackageSetBase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx.getAllScope;

public final class NamedScopeFilter implements VirtualFileFilter {
  private static final Logger LOG = Logger.getInstance(NamedScopeFilter.class);
  private final NamedScopesHolder holder;
  private final NamedScope scope;
  private final String string;

  public NamedScopeFilter(@NotNull NamedScopesHolder holder, @NotNull NamedScope scope) {
    this.holder = holder;
    this.scope = scope;
    this.string = scope + "; " + scope.getClass();
  }

  @NotNull
  public NamedScopesHolder getHolder() {
    return holder;
  }

  @NotNull
  public NamedScope getScope() {
    return scope;
  }

  @NotNull
  @Override
  public String toString() {
    return string;
  }

  @Override
  public boolean accept(VirtualFile file) {
    if (file == null) return false;

    PackageSet set = scope.getValue();
    if (set == null) return false;

    Project project = holder.getProject();
    if (set instanceof PackageSetBase) {
      PackageSetBase base = (PackageSetBase)set;
      return base.contains(file, project, holder);
    }
    PsiFile psiFile = PackageSetBase.getPsiFile(file, project);
    return psiFile != null && set.contains(psiFile, holder);
  }

  static boolean isVisible(@NotNull NamedScope scope) {
    return !(scope instanceof NonProjectFilesScope || scope instanceof ScratchesNamedScope || scope == getAllScope());
  }

  @NotNull
  static List<NamedScopeFilter> list(NamedScopesHolder... holders) {
    return list(NamedScopeFilter::isVisible, holders);
  }

  @NotNull
  static List<NamedScopeFilter> list(@NotNull Predicate<? super NamedScope> visible, @NotNull NamedScopesHolder... holders) {
    List<NamedScopeFilter> list = new ArrayList<>();
    for (NamedScopesHolder holder : holders) {
      for (NamedScope scope : holder.getScopes()) {
        String name = scope.getName();
        if (null == scope.getValue()) {
          LOG.debug("ignore scope without package set: ", name, "; holder: ", holder);
        }
        else if (!visible.test(scope)) {
          LOG.debug("ignore hidden scope: ", name, "; holder: ", holder);
        }
        else {
          list.add(new NamedScopeFilter(holder, scope));
        }
      }
    }
    return list;
  }
}
