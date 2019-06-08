/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k;

import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.refactoring.util.RefactoringChangeUtil;
import org.jetbrains.annotations.Nullable;


//copied from com.intellij.codeInsight.daemon.impl.quickfix.AddTypeArgumentsFix.addTypeArguments but code shortenClassReferences call removed
public class FixTypeArguments {
    @Nullable
    public static PsiExpression addTypeArguments(PsiExpression expression, PsiType toType) {
        if (!PsiUtil.isLanguageLevel5OrHigher(expression)) return null;

        if (expression instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCall = (PsiMethodCallExpression) expression;
            PsiReferenceParameterList list = methodCall.getMethodExpression().getParameterList();
            if (list == null || list.getTypeArguments().length > 0) return null;
            JavaResolveResult resolveResult = methodCall.resolveMethodGenerics();
            PsiElement element = resolveResult.getElement();
            if (element instanceof PsiMethod) {
                PsiMethod method = (PsiMethod) element;
                PsiType returnType = method.getReturnType();
                if (returnType == null) return null;

                PsiTypeParameter[] typeParameters = method.getTypeParameters();
                if (typeParameters.length > 0) {
                    PsiType[] mappings = PsiType.createArray(typeParameters.length);
                    PsiResolveHelper helper = JavaPsiFacade.getInstance(expression.getProject()).getResolveHelper();
                    LanguageLevel level = PsiUtil.getLanguageLevel(expression);
                    for (int i = 0; i < typeParameters.length; i++) {
                        PsiTypeParameter typeParameter = typeParameters[i];
                        PsiType substitution;
                        if (toType == null) {
                            substitution = resolveResult.getSubstitutor().substitute(typeParameter);
                            if (!PsiTypesUtil.isDenotableType(substitution, element)) return null;
                        }
                        else {
                            substitution = helper.getSubstitutionForTypeParameter(typeParameter, returnType, toType, false, level);
                        }
                        if (substitution == null || PsiType.NULL.equals(substitution)) return null;
                        mappings[i] = GenericsUtil.eliminateWildcards(substitution, false);
                    }

                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(expression.getProject());
                    PsiMethodCallExpression copy = (PsiMethodCallExpression) expression.copy();
                    PsiReferenceExpression methodExpression = copy.getMethodExpression();
                    PsiReferenceParameterList parameterList = methodExpression.getParameterList();
                    for (PsiType mapping : mappings) {
                        parameterList.add(factory.createTypeElement(mapping));
                    }
                    if (methodExpression.getQualifierExpression() == null) {
                        PsiExpression qualifierExpression;
                        PsiClass containingClass = method.getContainingClass();
                        if (method.hasModifierProperty(PsiModifier.STATIC)) {
                            qualifierExpression = factory.createReferenceExpression(containingClass);
                        }
                        else {
                            qualifierExpression = RefactoringChangeUtil.createThisExpression(method.getManager(), null);
                        }
                        methodExpression.setQualifierExpression(qualifierExpression);
                    }
                    return copy;
                }
            }
        }
        return null;
    }
}
