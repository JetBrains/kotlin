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
package org.jetbrains.plugins.gradle.service.resolve.dsl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames;
import org.jetbrains.plugins.gradle.service.resolve.GradleResolverUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import static org.jetbrains.plugins.gradle.service.resolve.GradleResolverUtil.canBeMethodOf;
import static org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter.MAP_KEY;

/**
 * @author Vladislav.Soroka
 */
public class GradleDslAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof GrReferenceExpression) {
      GrReferenceExpression referenceExpression = (GrReferenceExpression)element;
      final GrExpression qualifier = ResolveUtil.getSelfOrWithQualifier(referenceExpression);
      if (qualifier == null) return;
      if (qualifier instanceof GrReferenceExpression && ((GrReferenceExpression)qualifier).resolve() instanceof PsiClass) return;

      PsiType psiType = GradleResolverUtil.getTypeOf(qualifier);
      if (psiType == null) return;
      if (InheritanceUtil.isInheritor(psiType, GradleCommonClassNames.GRADLE_API_NAMED_DOMAIN_OBJECT_COLLECTION)) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(element.getProject());
        PsiClass defaultGroovyMethodsClass =
          javaPsiFacade.findClass(GroovyCommonClassNames.DEFAULT_GROOVY_METHODS, element.getResolveScope());
        if (canBeMethodOf(referenceExpression.getReferenceName(), defaultGroovyMethodsClass)) return;

        final String qualifiedName = TypesUtil.getQualifiedName(psiType);
        final PsiClass containerClass =
          qualifiedName != null ? javaPsiFacade.findClass(qualifiedName, element.getResolveScope()) : null;
        if (canBeMethodOf(referenceExpression.getReferenceName(), containerClass)) return;

        PsiElement nameElement = referenceExpression.getReferenceNameElement();
        if (nameElement != null) {
          holder.createInfoAnnotation(nameElement, null).setTextAttributes(MAP_KEY);
        }
      }
    }
  }
}
