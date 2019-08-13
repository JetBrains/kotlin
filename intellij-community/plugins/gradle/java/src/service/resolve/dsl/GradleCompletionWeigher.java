/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.service.resolve.dsl;

import com.intellij.codeInsight.completion.CompletionLocation;
import com.intellij.codeInsight.completion.CompletionWeigher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;

/**
 * @author Vladislav.Soroka
 */
public class GradleCompletionWeigher extends CompletionWeigher {

  @Override
  public Comparable weigh(@NotNull LookupElement element, @NotNull CompletionLocation location) {
    PsiFile containingFile = location.getCompletionParameters().getPosition().getContainingFile();
    if (!(containingFile instanceof GroovyFileBase)) {
      return null;
    }

    if (!FileUtilRt.extensionEquals(containingFile.getName(), GradleConstants.EXTENSION)) {
      return null;
    }

    Object o = element.getObject();
    if (o instanceof ResolveResult) {
      PsiElement psiElement = ((ResolveResult)o).getElement();
      PsiFile psiFile = null;
      if (psiElement != null) {
        psiFile = psiElement.getContainingFile();
      }
      if (psiFile == null && psiElement instanceof PsiMember) {
        PsiClass psiClass = ((PsiMember)psiElement).getContainingClass();
        psiFile = psiClass != null ? psiClass.getContainingFile() : null;
      }
      if (psiFile instanceof PsiJavaFile) {
        if (((PsiJavaFile)psiFile).getPackageName().startsWith("org.gradle")) {
          return 1;
        }
      }
    }
    return -1;
  }
}
