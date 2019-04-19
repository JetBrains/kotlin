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
package org.jetbrains.plugins.gradle.service.resolve;

import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleLog;
import org.jetbrains.plugins.groovy.lang.parser.GroovyElementTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightParameter;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;

import java.util.Arrays;

/**
 * @author Vladislav.Soroka
 */
public class GradleResolverUtil {

  public static int getGrMethodArumentsCount(@NotNull GrArgumentList args) {
    int argsCount = 0;
    boolean namedArgProcessed = false;
    for (GroovyPsiElement arg : args.getAllArguments()) {
      if (arg instanceof GrNamedArgument) {
        if (!namedArgProcessed) {
          namedArgProcessed = true;
          argsCount++;
        }
      }
      else {
        argsCount++;
      }
    }
    return argsCount;
  }

  @Nullable
  public static GrLightMethodBuilder createMethodWithClosure(@NotNull String name,
                                                             @Nullable String returnType,
                                                             @Nullable String closureTypeParameter,
                                                             @NotNull PsiElement place) {
    PsiClassType closureType;
    PsiClass closureClass =
      JavaPsiFacade.getInstance(place.getProject()).findClass(GroovyCommonClassNames.GROOVY_LANG_CLOSURE, place.getResolveScope());
    if (closureClass == null) return null;

    if (closureClass.getTypeParameters().length != 1) {
      GradleLog.LOG.debug(String.format("Unexpected type parameters found for closureClass(%s) : (%s)",
                                        closureClass, Arrays.toString(closureClass.getTypeParameters())));
      return null;
    }

    PsiElementFactory factory = JavaPsiFacade.getElementFactory(place.getManager().getProject());

    if (closureTypeParameter != null) {
      PsiClassType closureClassTypeParameter =
        factory.createTypeByFQClassName(closureTypeParameter, place.getResolveScope());
      closureType = factory.createType(closureClass, closureClassTypeParameter);
    }
    else {
      PsiClassType closureClassTypeParameter =
        factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT, place.getResolveScope());
      closureType = factory.createType(closureClass, closureClassTypeParameter);
    }

    GrLightMethodBuilder methodWithClosure = new GrLightMethodBuilder(place.getManager(), name);
    GrLightParameter closureParameter = new GrLightParameter("closure", closureType, methodWithClosure);
    methodWithClosure.addParameter(closureParameter);
    PsiClassType retType = factory.createTypeByFQClassName(
      returnType != null ? returnType : CommonClassNames.JAVA_LANG_OBJECT, place.getResolveScope());
    methodWithClosure.setReturnType(retType);
    methodWithClosure.setContainingClass(retType.resolve());
    return methodWithClosure;
  }

  public static boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                            @NotNull ResolveState state,
                                            @NotNull PsiElement place,
                                            @NotNull String... fqNames) {
    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(place.getProject());
    for (String fqName : fqNames) {
      if(fqName == null) continue;
      PsiClass psiClass = javaPsiFacade.findClass(fqName, place.getResolveScope());
      if (psiClass != null) {
        if (!UtilKt.processDeclarations(psiClass, processor, state, place)) return false;
      }
    }
    return true;
  }

  @Nullable
  public static PsiElement findParent(@NotNull PsiElement element, int level) {
    PsiElement parent = element;
    do {
      parent = parent.getParent();
    }
    while (parent != null && --level > 0);
    return parent;
  }

  @Nullable
  public static <T extends PsiElement> T findParent(@NotNull PsiElement element, Class<T> clazz) {
    PsiElement parent = element;
    do {
      parent = parent.getParent();
      if (clazz.isInstance(parent)) {
        //noinspection unchecked
        return (T)parent;
      }
    }
    while (parent != null && !(parent instanceof GroovyFile));
    return null;
  }

  public static boolean canBeMethodOf(@Nullable String methodName, @Nullable PsiClass aClass) {
    return methodName != null && aClass != null && aClass.findMethodsByName(methodName, true).length != 0;
  }

  @Nullable
  public static PsiType getTypeOf(@Nullable final GrExpression expression) {
    if (expression == null) return null;
    return RecursionManager.doPreventingRecursion(expression, true, () -> expression.getNominalType());
  }

  public static boolean isLShiftElement(@Nullable PsiElement psiElement) {
    return (psiElement instanceof GrBinaryExpression &&
            GroovyElementTypes.COMPOSITE_LSHIFT_SIGN.equals(((GrBinaryExpression)psiElement).getOperationTokenType()));
  }
}
