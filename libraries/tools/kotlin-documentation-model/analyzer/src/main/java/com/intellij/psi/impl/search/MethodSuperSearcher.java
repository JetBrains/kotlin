/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package com.intellij.psi.impl.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.searches.SuperMethodsSearch;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

// A verbatim copy of `com.intellij.psi.impl.search.MethodSuperSearcher` from the IntelliJ platform.
//
// This is the only executor of the `com.intellij.superMethodsSearch` extension point, which
// `PsiMethod.findSuperMethods()` delegates to. Up to platform 251 the class shipped in `java-psi-impl` and
// was registered by that artifact's `META-INF/JavaPsiPlugin.xml`. In 251 the descriptor was renamed to
// `intellij.java.psi.impl.xml`, and both the class and its registration moved to `java-indexing-impl`
// (`META-INF/JavaIndexingPlugin.xml`) -- an artifact Dokka does not depend on, and one that would drag in
// the whole indexing infrastructure. With no executor the extension point yields nothing and
// `findSuperMethods()` silently returns an empty array.
//
// Copying the class is enough because it is plain PSI: everything it touches
// (`HierarchicalMethodSignature`, `MethodSignatureUtil`, `InheritanceUtil`, `JavaPsiFacade`) is already on
// Dokka's classpath, and it needs no index. It is registered into the extension point by
// `org.jetbrains.dokka.analysis.java.registerJavaPsiSearchExecutors`.
//
// Keep this file in sync when the IntelliJ platform version is bumped.
public final class MethodSuperSearcher extends QueryExecutorBase<MethodSignatureBackedByPsiMethod, SuperMethodsSearch.SearchParameters> {
  private static final Logger LOG = Logger.getInstance(MethodSuperSearcher.class);

  public MethodSuperSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull SuperMethodsSearch.SearchParameters queryParameters,
                           @NotNull Processor<? super MethodSignatureBackedByPsiMethod> consumer) {
    PsiClass parentClass = queryParameters.getPsiClass();
    PsiMethod method = queryParameters.getMethod();
    HierarchicalMethodSignature signature = method.getHierarchicalMethodSignature();

    boolean checkBases = queryParameters.isCheckBases();
    boolean allowStaticMethod = queryParameters.isAllowStaticMethod();
    List<HierarchicalMethodSignature> supers = signature.getSuperSignatures();
    for (HierarchicalMethodSignature superSignature : supers) {
      if (MethodSignatureUtil.isSubsignature(superSignature, signature)) {
        if (!addSuperMethods(superSignature, method, parentClass, allowStaticMethod, checkBases, consumer)) return;
      }
    }
  }

  private static boolean addSuperMethods(final HierarchicalMethodSignature signature,
                                         final PsiMethod method,
                                         final PsiClass parentClass,
                                         final boolean allowStaticMethod,
                                         final boolean checkBases,
                                         final Processor<? super MethodSignatureBackedByPsiMethod> consumer) {
    PsiMethod signatureMethod = signature.getMethod();
    PsiClass hisClass = signatureMethod.getContainingClass();
    if (parentClass == null || InheritanceUtil.isInheritorOrSelf(parentClass, hisClass, true)) {
      if (isAcceptable(signatureMethod, method, allowStaticMethod)) {
        if (parentClass != null && !parentClass.equals(hisClass) && !checkBases) {
          return true;
        }
        LOG.assertTrue(signatureMethod != method, method); // method != method.getsuper()
        return consumer.process(signature); //no need to check super classes
      }
    }
    for (HierarchicalMethodSignature superSignature : signature.getSuperSignatures()) {
      if (MethodSignatureUtil.isSubsignature(superSignature, signature)) {
        addSuperMethods(superSignature, method, parentClass, allowStaticMethod, checkBases, consumer);
      }
    }

    return true;
  }

  private static boolean isAcceptable(final PsiMethod superMethod, final PsiMethod method, final boolean allowStaticMethod) {
    boolean hisStatic = superMethod.hasModifierProperty(PsiModifier.STATIC);
    return hisStatic == method.hasModifierProperty(PsiModifier.STATIC) &&
           (allowStaticMethod || !hisStatic) &&
           JavaPsiFacade.getInstance(method.getProject()).getResolveHelper().isAccessible(superMethod, method, null);
  }
}
