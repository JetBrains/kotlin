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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings;
import org.jetbrains.plugins.groovy.extensions.GroovyMapContentProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiManager;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrReferenceTypeEnhancer;

/**
 * @author Vladislav.Soroka
 */
public class NamedDomainObjectCollectionTypeEnhancer extends GrReferenceTypeEnhancer {
  @Override
  public PsiType getReferenceType(GrReferenceExpression ref, @Nullable PsiElement resolved) {
    if (resolved != null) return null;

    GrExpression qualifierExpression = ref.getQualifierExpression();
    if (qualifierExpression == null) return null;

    PsiType namedDomainCollectionType = GradleResolverUtil.getTypeOf(qualifierExpression);

    if (!InheritanceUtil.isInheritor(namedDomainCollectionType, GradleCommonClassNames.GRADLE_API_NAMED_DOMAIN_OBJECT_COLLECTION)) {
      return null;
    }

    PsiElement qResolved;

    if (qualifierExpression instanceof GrReferenceExpression) {
      qResolved = ((GrReferenceExpression)qualifierExpression).resolve();
    }
    else if (qualifierExpression instanceof GrMethodCall) {
      qResolved = ((GrMethodCall)qualifierExpression).resolveMethod();
    }
    else {
      return null;
    }

    String key = ref.getReferenceName();
    if (key == null) return null;

    for (GroovyMapContentProvider provider : GroovyMapContentProvider.EP_NAME.getExtensions()) {
      PsiType type = provider.getValueType(qualifierExpression, qResolved, key);
      if (type != null) {
        return type;
      }
    }

    if (namedDomainCollectionType instanceof PsiClassReferenceType) {
      final PsiClassReferenceType referenceType = (PsiClassReferenceType)namedDomainCollectionType;
      final String fqName = TypesUtil.getQualifiedName(referenceType);
      if (GradleCommonClassNames.GRADLE_API_SOURCE_SET_CONTAINER.equals(fqName)) {
        final GroovyPsiManager psiManager = GroovyPsiManager.getInstance(ref.getProject());
        return psiManager.createTypeByFQClassName(GradleCommonClassNames.GRADLE_API_SOURCE_SET, ref.getResolveScope());
      }
      else if (GradleCommonClassNames.GRADLE_API_CONFIGURATION_CONTAINER.equals(fqName)) {
        final GroovyPsiManager psiManager = GroovyPsiManager.getInstance(ref.getProject());
        return psiManager.createTypeByFQClassName(GradleCommonClassNames.GRADLE_API_CONFIGURATION, ref.getResolveScope());
      }
      else if (GradleCommonClassNames.GRADLE_API_TASK_CONTAINER.equals(fqName)) {
        final GroovyPsiManager psiManager = GroovyPsiManager.getInstance(ref.getProject());
        return psiManager.createTypeByFQClassName(GradleCommonClassNames.GRADLE_API_TASK, ref.getResolveScope());
      }
      else if (GradleCommonClassNames.GRADLE_API_DISTRIBUTION_CONTAINER.equals(fqName)) {
        final GroovyPsiManager psiManager = GroovyPsiManager.getInstance(ref.getProject());
        return psiManager.createTypeByFQClassName(GradleCommonClassNames.GRADLE_API_DISTRIBUTION, ref.getResolveScope());
      }
      else {
        GradleExtensionsSettings.GradleExtensionsData extensionsData = GradleExtensionsContributor.Companion.getExtensionsFor(ref);
        if (extensionsData != null) {
          for (GradleExtensionsSettings.GradleExtension extension : extensionsData.extensions.values()) {
            if (StringUtil.isNotEmpty(extension.namedObjectTypeFqn) && extension.rootTypeFqn.equals(fqName)) {
              final GroovyPsiManager psiManager = GroovyPsiManager.getInstance(ref.getProject());
              return psiManager.createTypeByFQClassName(extension.namedObjectTypeFqn, ref.getResolveScope());
            }
          }
        }
      }
    }

    return null;
  }
}
