// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.impl.source.PsiFileWithStubSupport;
import org.jetbrains.jps.backwardRefs.NameEnumerator;
import org.jetbrains.jps.backwardRefs.CompilerRef;

import java.util.Collection;

public enum CompilerHierarchySearchType {
  DIRECT_INHERITOR {
    @Override
    PsiElement[] performSearchInFile(SearchId[] definitions,
                                     PsiNamedElement baseElement,
                                     PsiFileWithStubSupport file,
                                     LanguageCompilerRefAdapter adapter) {
      return adapter.findDirectInheritorCandidatesInFile(definitions, file);
    }

    @Override
    Class<? extends CompilerRef> getRequiredClass(LanguageCompilerRefAdapter adapter) {
      return adapter.getHierarchyObjectClass();
    }

    @Override
    SearchId[] convertToIds(Collection<CompilerRef> compilerRef, NameEnumerator nameEnumerator) {
      return compilerRef.stream().map(r -> r instanceof CompilerRef.JavaCompilerAnonymousClassRef
             ? new SearchId(((CompilerRef.JavaCompilerAnonymousClassRef)r).getName())
             : new SearchId(nameEnumerator.getName(((CompilerRef.CompilerClassHierarchyElementDef)r).getName()))).toArray(SearchId[]::new);
    }
  },
  FUNCTIONAL_EXPRESSION {
    @Override
    PsiElement[] performSearchInFile(SearchId[] definitions,
                                     PsiNamedElement baseElement,
                                     PsiFileWithStubSupport file,
                                     LanguageCompilerRefAdapter adapter) {
      return adapter.findFunExpressionsInFile(definitions, file);
    }

    @Override
    Class<? extends CompilerRef> getRequiredClass(LanguageCompilerRefAdapter adapter) {
      return adapter.getFunExprClass();
    }

    @Override
    SearchId[] convertToIds(Collection<CompilerRef> compilerRef, NameEnumerator nameEnumerator) {
      return compilerRef.stream().map(r -> ((CompilerRef.CompilerFunExprDef) r).getId()).map(SearchId::new).toArray(SearchId[]::new);
    }
  };

  abstract PsiElement[] performSearchInFile(SearchId[] definitions,
                                            PsiNamedElement baseElement,
                                            PsiFileWithStubSupport file,
                                            LanguageCompilerRefAdapter adapter);

  abstract Class<? extends CompilerRef> getRequiredClass(LanguageCompilerRefAdapter adapter);

  abstract SearchId[] convertToIds(Collection<CompilerRef> compilerRef, NameEnumerator nameEnumerator);

}
