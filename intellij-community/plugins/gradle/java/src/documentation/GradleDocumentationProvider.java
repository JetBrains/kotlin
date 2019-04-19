/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.documentation;

import com.intellij.codeInsight.javadoc.JavaDocUtil;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleDocumentationBundle;
import org.jetbrains.plugins.groovy.dsl.CustomMembersGenerator;
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleDocumentationProvider implements DocumentationProvider {

  @Nullable
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    PsiFile file = element.getContainingFile();
    if (file == null || !FileUtilRt.extensionEquals(file.getName(), GradleConstants.EXTENSION)) return null;
    if (element instanceof GrLightVariable) {
      PsiElement navigationElement = element.getNavigationElement();
      if (navigationElement != null) {
        String doc = navigationElement.getUserData(NonCodeMembersHolder.DOCUMENTATION);
        if (doc != null) return doc;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Nullable
  @Override
  public String generateDoc(PsiElement element, PsiElement originalElement) {
    PsiFile file = element.getContainingFile();
    if (file == null || !FileUtilRt.extensionEquals(file.getName(), GradleConstants.EXTENSION)) return null;
    return element instanceof GrLiteral ? findDoc(element, ((GrLiteral)element).getValue()) : null;
  }

  @Nullable
  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    final PsiFile file = element.getContainingFile();
    if (file == null || !FileUtilRt.extensionEquals(file.getName(), GradleConstants.EXTENSION)) return null;
    final String doc = findDoc(element, object);
    return !StringUtil.isEmpty(doc) ? new CustomMembersGenerator.GdslNamedParameter(String.valueOf(object), doc, element, null) : null;
  }

  @Nullable
  @Override
  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
    return JavaDocUtil.findReferenceTarget(psiManager, link, context);
  }

  @Nullable
  private static String findDoc(@Nullable PsiElement element, Object argValue) {
    String result = null;
    if (element instanceof GrLiteral) {
      GrLiteral grLiteral = (GrLiteral)element;
      PsiElement stmt = PsiTreeUtil.findFirstParent(grLiteral, psiElement -> psiElement instanceof GrCall);
      if (stmt instanceof GrCall) {
        GrCall grCall = (GrCall)stmt;
        PsiMethod psiMethod = grCall.resolveMethod();
        if (psiMethod != null && psiMethod.getContainingClass() != null) {
          String qualifiedName = psiMethod.getContainingClass().getQualifiedName();
          if (grLiteral.getParent() instanceof GrNamedArgument) {
            GrNamedArgument namedArgument = (GrNamedArgument)grLiteral.getParent();
            String key = StringUtil.join(new String[]{
              "gradle.documentation",
              qualifiedName,
              psiMethod.getName(),
              namedArgument.getLabelName(),
              String.valueOf(argValue),
            }, "."
            );

            result = GradleDocumentationBundle.messageOrDefault(key, "");
          }
        }
      }
    }
    return result;
  }
}
