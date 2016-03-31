/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.inspections.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.RegistrationDetector;
import com.android.tools.lint.detector.api.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.*;
import lombok.ast.AstVisitor;
import lombok.ast.CompilationUnit;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static com.android.SdkConstants.*;

/**
 * Intellij-specific version of the {@link com.android.tools.lint.checks.RegistrationDetector} which uses the PSI structure
 * to check classes.
 * <p>
 * <ul>
 *   <li>Unit tests, and compare to the bytecode based results</li>
 * </ul>
 */
public class IntellijRegistrationDetector extends RegistrationDetector implements Detector.JavaScanner {
  static final Implementation IMPLEMENTATION = new Implementation(
    IntellijRegistrationDetector.class,
    EnumSet.of(Scope.MANIFEST, Scope.JAVA_FILE));

  @Nullable
  @Override
  public List<Class<? extends Node>> getApplicableNodeTypes() {
    return Collections.<Class<? extends Node>>singletonList(CompilationUnit.class);
  }

  @Nullable
  @Override
  public AstVisitor createJavaVisitor(@NonNull final JavaContext context) {
    return new ForwardingAstVisitor() {
      @Override
      public boolean visitCompilationUnit(CompilationUnit node) {
        check(context);
        return true;
      }
    };
  }

  private void check(final JavaContext context) {
    ApplicationManager.getApplication().runReadAction(new Runnable() {
      @Override
      public void run() {
        final PsiFile psiFile = IntellijLintUtils.getPsiFile(context);
        if (!(psiFile instanceof PsiJavaFile)) {
          return;
        }
        PsiJavaFile javaFile = (PsiJavaFile)psiFile;
        for (PsiClass clz : javaFile.getClasses()) {
          check(context, clz);
        }
      }
    });
  }

  private void check(JavaContext context, PsiClass clz) {
    for (PsiClass current = clz.getSuperClass(); current != null; current = current.getSuperClass()) {
      // Ignore abstract classes
      if (clz.hasModifierProperty(PsiModifier.ABSTRACT) || clz instanceof PsiAnonymousClass) {
        continue;
      }
      String fqcn = current.getQualifiedName();
      if (fqcn == null) {
        continue;
      }
      if (CLASS_ACTIVITY.equals(fqcn)
          || CLASS_SERVICE.equals(fqcn)
          || CLASS_CONTENTPROVIDER.equals(fqcn)) {

        String internalName = IntellijLintUtils.getInternalName(clz);
        if (internalName == null) {
          continue;
        }
        String frameworkClass = ClassContext.getInternalName(fqcn);
        Collection<String> registered = mManifestRegistrations != null ? mManifestRegistrations.get(frameworkClass) : null;
        if (registered == null || !registered.contains(internalName)) {
          report(context, clz, frameworkClass);
        }
        break;
      }
    }

    for (PsiClass innerClass : clz.getInnerClasses()) {
      check(context, innerClass);
    }
  }

  private static void report(JavaContext context, PsiClass clz, String internalName) {
    // Unlike the superclass, we don't have to check that the tags are compatible;
    // IDEA already checks that as part of its XML validation

    if (IntellijLintUtils.isSuppressed(clz, clz.getContainingFile(), ISSUE)) {
      return;
    }
    String tag = classToTag(internalName);
    Location location = IntellijLintUtils.getLocation(context.file, clz);
    String fqcn = clz.getQualifiedName();
    if (fqcn == null) {
      fqcn = clz.getName();
    }
    context.report(ISSUE, location, String.format("The <%1$s> %2$s is not registered in the manifest", tag, fqcn), null);
  }
}
