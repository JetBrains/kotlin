// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve;

import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyUnresolvedHighlightFilter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.Set;

import static com.intellij.util.containers.ContainerUtil.newHashSet;
import static org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_EXTRA_PROPERTIES_EXTENSION;
import static org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createGenericType;

/**
 * @author Vladislav.Soroka
 */
public class GradleUnresolvedReferenceFilter extends GroovyUnresolvedHighlightFilter {

  private final static Set<String> IGNORE_SET = newHashSet(
    GradleCommonClassNames.GRADLE_API_TASK,
    GradleCommonClassNames.GRADLE_API_SOURCE_SET,
    GradleCommonClassNames.GRADLE_API_CONFIGURATION,
    GradleCommonClassNames.GRADLE_API_DISTRIBUTION
  );

  @Override
  public boolean isReject(@NotNull GrReferenceExpression expression) {
    final PsiType psiType = GradleResolverUtil.getTypeOf(expression);
    if (psiType == null) {
      PsiElement child = expression.getFirstChild();
      if (child == null) return false;
      PsiReference reference = child.getReference();
      if (reference instanceof GrReferenceExpression) {
        PsiType type = ((GrReferenceExpression)reference).getType();
        if (type != null) {
          PsiClassType extType = createGenericType(GRADLE_API_EXTRA_PROPERTIES_EXTENSION, expression, null);
          return TypeConversionUtil.areTypesConvertible(type, extType);
        }
      }
      return false;
    }

    return false;
  }
}
