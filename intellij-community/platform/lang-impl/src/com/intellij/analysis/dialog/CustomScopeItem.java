/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.analysis.dialog;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.find.FindSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CustomScopeItem implements ModelScopeItem {
  private final Project myProject;
  private boolean mySearchInLib;
  private String myPreselect;
  private Supplier<? extends SearchScope> mySupplierScope;

  public CustomScopeItem(Project project, @Nullable PsiElement context) {
    myProject = project;

    AnalysisUIOptions options = AnalysisUIOptions.getInstance(project);
    VirtualFile file = PsiUtilCore.getVirtualFile(context);
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    mySearchInLib = file != null && fileIndex.isInLibrary(file);

    myPreselect = StringUtil.isEmptyOrSpaces(options.CUSTOM_SCOPE_NAME)
                       ? FindSettings.getInstance().getDefaultScopeName()
                       : options.CUSTOM_SCOPE_NAME;
    if (mySearchInLib && GlobalSearchScope.projectScope(myProject).getDisplayName().equals(myPreselect)) {
      myPreselect = GlobalSearchScope.allScope(myProject).getDisplayName();
    }
    if (GlobalSearchScope.allScope(myProject).getDisplayName().equals(myPreselect) && options.SCOPE_TYPE == AnalysisScope.CUSTOM) {
      options.CUSTOM_SCOPE_NAME = myPreselect;
      mySearchInLib = true;
    }
  }

  public Project getProject() { return myProject; }

  public boolean getSearchInLibFlag() {
    return mySearchInLib;
  }

  public String getPreselectedCustomScope() {
    return myPreselect;
  }

  @Override
  public AnalysisScope getScope() {
    if (mySupplierScope != null)
      return new AnalysisScope(mySupplierScope.get(), myProject);
    return null;
  }

  public void setSearchScopeSupplier(Supplier<? extends SearchScope> supplier) {
    mySupplierScope = supplier;
  }
}
