// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs;

import com.intellij.compiler.CompilerDirectHierarchyInfo;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.PsiFileWithStubSupport;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Stream;

class CompilerHierarchyInfoImpl implements CompilerDirectHierarchyInfo {
  private final static Logger LOG = Logger.getInstance(CompilerHierarchyInfoImpl.class);

  private final PsiNamedElement myBaseClass;
  private final GlobalSearchScope myDirtyScope;
  private final GlobalSearchScope mySearchScope;
  private final Project myProject;
  private final FileType mySearchFileType;
  private final CompilerHierarchySearchType mySearchType;
  private final Map<VirtualFile, SearchId[]> myCandidatePerFile;

  CompilerHierarchyInfoImpl(Map<VirtualFile, SearchId[]> candidatesPerFile,
                            PsiNamedElement baseClass,
                            GlobalSearchScope dirtyScope,
                            GlobalSearchScope searchScope,
                            Project project,
                            FileType searchFileType,
                            CompilerHierarchySearchType searchType) {
    myCandidatePerFile = candidatesPerFile;
    myBaseClass = baseClass;
    myDirtyScope = dirtyScope;
    mySearchScope = searchScope;
    myProject = project;
    mySearchFileType = searchFileType;
    mySearchType = searchType;
  }

  @Override
  @NotNull
  public Stream<PsiElement> getHierarchyChildren() {
    PsiManager psiManager = PsiManager.getInstance(myProject);
    final LanguageCompilerRefAdapter adapter = ObjectUtils.notNull(LanguageCompilerRefAdapter.findAdapter(mySearchFileType));
    return myCandidatePerFile
      .entrySet()
      .stream()
      .filter(e -> mySearchScope.contains(e.getKey()))
      .flatMap(e -> {
        final VirtualFile file = e.getKey();
        final SearchId[] definitions = e.getValue();

        final PsiElement[] hierarchyChildren = ReadAction.compute(() -> {
          final PsiFileWithStubSupport psiFile = (PsiFileWithStubSupport)psiManager.findFile(file);
          return mySearchType.performSearchInFile(definitions, myBaseClass, psiFile, adapter);
        });

        if (hierarchyChildren.length == definitions.length) {
          return Stream.of(hierarchyChildren);
        }
        else {
          LOG.assertTrue(mySearchType == CompilerHierarchySearchType.DIRECT_INHERITOR, "Should not happens for functional expression search");
          return Stream.of(hierarchyChildren).filter(c -> ReadAction.compute(() -> adapter.isDirectInheritor(c, myBaseClass)));
        }
      });
  }

  @Override
  @NotNull
  public GlobalSearchScope getDirtyScope() {
    return myDirtyScope;
  }
}
