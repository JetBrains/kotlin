/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.service.resolve;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings;
import org.jetbrains.plugins.groovy.extensions.GroovyUnresolvedHighlightFilter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;

import java.util.HashSet;
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

    Set<String> toIgnore = new HashSet<>(IGNORE_SET);
    GradleExtensionsSettings.GradleExtensionsData extensionsData = GradleExtensionsContributor.Companion.getExtensionsFor(expression);
    if (extensionsData != null) {
      for (GradleExtensionsSettings.GradleExtension extension : extensionsData.extensions.values()) {
        if (StringUtil.isNotEmpty(extension.namedObjectTypeFqn)) {
          toIgnore.add(extension.namedObjectTypeFqn);
        }
      }
    }
    return toIgnore.contains(TypesUtil.getQualifiedName(psiType));
  }
}
