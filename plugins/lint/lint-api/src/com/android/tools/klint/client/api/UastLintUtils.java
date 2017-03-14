/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.tools.klint.client.api;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.android.tools.klint.detector.api.ConstantEvaluator;
import com.android.tools.klint.detector.api.JavaContext;
import com.google.common.base.Joiner;
import com.intellij.psi.*;

import org.jetbrains.uast.*;
import org.jetbrains.uast.UReferenceExpression;
import org.jetbrains.uast.java.JavaAbstractUExpression;
import org.jetbrains.uast.java.JavaUDeclarationsExpression;

import java.util.Collections;
import java.util.List;

public class UastLintUtils {
    @Nullable
    public static String getQualifiedName(PsiElement element) {
        if (element instanceof PsiClass) {
            return ((PsiClass) element).getQualifiedName();
        } else if (element instanceof PsiMethod) {
            PsiClass containingClass = ((PsiMethod) element).getContainingClass();
            if (containingClass == null) {
                return null;
            }
            String containingClassFqName = getQualifiedName(containingClass);
            if (containingClassFqName == null) {
                return null;
            }
            return containingClassFqName + "." + ((PsiMethod) element).getName();
        } else if (element instanceof PsiField) {
            PsiClass containingClass = ((PsiField) element).getContainingClass();
            if (containingClass == null) {
                return null;
            }
            String containingClassFqName = getQualifiedName(containingClass);
            if (containingClassFqName == null) {
                return null;
            }
            return containingClassFqName + "." + ((PsiField) element).getName();
        } else {
            return null;
        }
    }

    @Nullable
    public static PsiElement resolve(ExternalReferenceExpression expression, UElement context) {
        UDeclaration declaration = UastUtils.getParentOfType(context, UDeclaration.class);
        if (declaration == null) {
            return null;
        }

        return expression.resolve(declaration.getPsi());
    }

    @NonNull
    public static String getClassName(PsiClassType type) {
        PsiClass psiClass = type.resolve();
        if (psiClass == null) {
            return type.getClassName();
        } else {
            return getClassName(psiClass);
        }
    }
    
    @NonNull
    public static String getClassName(PsiClass psiClass) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(psiClass.getName());
        psiClass = psiClass.getContainingClass();
        while (psiClass != null) {
            stringBuilder.insert(0, psiClass.getName() + ".");
            psiClass = psiClass.getContainingClass();
        }
        return stringBuilder.toString();
    }
    
    @Nullable
    public static UExpression findLastAssignment(
            @NonNull PsiVariable variable,
            @NonNull UElement call,
            @NonNull JavaContext context) {
        UElement lastAssignment = null;
        
        if (variable instanceof UVariable) {
            variable = ((UVariable) variable).getPsi();
        }
        
        if (!variable.hasModifierProperty(PsiModifier.FINAL) &&
                (variable instanceof PsiLocalVariable || variable instanceof PsiParameter)) {
            UMethod containingFunction = UastUtils.getContainingUMethod(call);
            if (containingFunction != null) {
                ConstantEvaluator.LastAssignmentFinder finder =
                        new ConstantEvaluator.LastAssignmentFinder(variable, call, context, null, -1);
                containingFunction.accept(finder);
                lastAssignment = finder.getLastAssignment();
            }
        } else {
            lastAssignment = context.getUastContext().getInitializerBody(variable);
        }

        if (lastAssignment instanceof UExpression) {
            return (UExpression) lastAssignment;
        }

        return null;
    }
    
    @Nullable
    public static String getReferenceName(UReferenceExpression expression) {
        if (expression instanceof USimpleNameReferenceExpression) {
            return ((USimpleNameReferenceExpression) expression).getIdentifier();
        } else if (expression instanceof UQualifiedReferenceExpression) {
            UExpression selector = ((UQualifiedReferenceExpression) expression).getSelector();
            if (selector instanceof USimpleNameReferenceExpression) {
                return ((USimpleNameReferenceExpression) selector).getIdentifier();
            }
        }
        
        return null;
    } 
    
    @Nullable
    public static Object findLastValue(
            @NonNull PsiVariable variable,
            @NonNull UElement call,
            @NonNull JavaContext context,
            @NonNull ConstantEvaluator evaluator) {
        Object value = null;
        
        if (!variable.hasModifierProperty(PsiModifier.FINAL) &&
                (variable instanceof PsiLocalVariable || variable instanceof PsiParameter)) {
            UMethod containingFunction = UastUtils.getContainingUMethod(call);
            if (containingFunction != null) {
                ConstantEvaluator.LastAssignmentFinder
                        finder = new ConstantEvaluator.LastAssignmentFinder(
                        variable, call, context, evaluator, 1);
                containingFunction.getUastBody().accept(finder);
                value = finder.getCurrentValue();
            }
        } else {
            UExpression initializer = context.getUastContext().getInitializerBody(variable);
            if (initializer != null) {
                value = initializer.evaluate();
            }
        }

        return value;
    }

    @Nullable
    private static AndroidReference toAndroidReference(UQualifiedReferenceExpression expression) {
        List<String> path = UastUtils.asQualifiedPath(expression);
        
        String packageNameFromResolved = null;

        PsiClass containingClass = UastUtils.getContainingClass(expression.resolve());
        if (containingClass != null) {
            String containingClassFqName = containingClass.getQualifiedName();
            
            if (containingClassFqName != null) {
                int i = containingClassFqName.lastIndexOf(".R.");
                if (i >= 0) {
                    packageNameFromResolved = containingClassFqName.substring(0, i);
                }
            }
        }

        if (path == null) {
            return null;
        }

        int size = path.size();
        if (size < 3) {
            return null;
        }

        String r = path.get(size - 3);
        if (!r.equals(SdkConstants.R_CLASS)) {
            return null;
        }

        String packageName = packageNameFromResolved != null
                ? packageNameFromResolved
                : Joiner.on('.').join(path.subList(0, size - 3));

        String type = path.get(size - 2);
        String name = path.get(size - 1);

        ResourceType resourceType = null;
        for (ResourceType value : ResourceType.values()) {
            if (value.getName().equals(type)) {
                resourceType = value;
                break;
            }
        }

        if (resourceType == null) {
            return null;
        }

        return new AndroidReference(expression, packageName, resourceType, name);
    }


    @Nullable
    public static AndroidReference toAndroidReferenceViaResolve(UElement element) {
        if (element instanceof UQualifiedReferenceExpression
                && element instanceof JavaAbstractUExpression) {
            AndroidReference ref = toAndroidReference((UQualifiedReferenceExpression) element);
            if (ref != null) {
                return ref;
            }
        }

        PsiElement declaration;
        if (element instanceof UVariable) {
            declaration = ((UVariable) element).getPsi();
        } else if (element instanceof UResolvable) {
            declaration = ((UResolvable) element).resolve();
        } else {
            return null;
        }
        
        if (declaration == null && element instanceof USimpleNameReferenceExpression 
                && element instanceof JavaAbstractUExpression) {
            // R class can't be resolved in tests so we need to use heuristics to calc the reference 
            UExpression maybeQualified = UastUtils.getQualifiedParentOrThis((UExpression) element);
            if (maybeQualified instanceof UQualifiedReferenceExpression) {
                AndroidReference ref = toAndroidReference(
                        (UQualifiedReferenceExpression) maybeQualified);
                if (ref != null) {
                    return ref;
                }
            }
        }
        
        if (!(declaration instanceof PsiVariable)) {
            return null;
        }
        
        PsiVariable variable = (PsiVariable) declaration;
        if (!(variable instanceof PsiField) 
                || variable.getType() != PsiType.INT
                || !variable.hasModifierProperty(PsiModifier.STATIC) 
                || !variable.hasModifierProperty(PsiModifier.FINAL)) {
            return null;
        }
        
        PsiClass resTypeClass = ((PsiField) variable).getContainingClass();
        if (resTypeClass == null || !resTypeClass.hasModifierProperty(PsiModifier.STATIC)) {
            return null;
        }
        
        PsiClass rClass = resTypeClass.getContainingClass();
        if (rClass == null || rClass.getContainingClass() != null || !"R".equals(rClass.getName())) {
            return null;
        }
        
        String packageName = ((PsiJavaFile) rClass.getContainingFile()).getPackageName();
        if (packageName.isEmpty()) {
            return null;
        }

        String resourceTypeName = resTypeClass.getName();
        ResourceType resourceType = null;
        for (ResourceType value : ResourceType.values()) {
            if (value.getName().equals(resourceTypeName)) {
                resourceType = value;
                break;
            }
        }

        if (resourceType == null) {
            return null;
        }

        String resourceName = variable.getName();

        UExpression node;
        if (element instanceof UExpression) {
            node = (UExpression) element;
        } else if (element instanceof UVariable) {
            node = new JavaUDeclarationsExpression(
                    null, Collections.singletonList(((UVariable) element)));
        } else {
            throw new IllegalArgumentException("element must be an expression or an UVariable");
        }

        return new AndroidReference(node, packageName, resourceType, resourceName);
    }

    public static boolean areIdentifiersEqual(UExpression first, UExpression second) {
        String firstIdentifier = getIdentifier(first);
        String secondIdentifier = getIdentifier(second);
        return firstIdentifier != null && secondIdentifier != null
                && firstIdentifier.equals(secondIdentifier);
    }

    @Nullable
    public static String getIdentifier(UExpression expression) {
        if (expression instanceof ULiteralExpression) {
            expression.asRenderString();
        } else if (expression instanceof USimpleNameReferenceExpression) {
            return ((USimpleNameReferenceExpression) expression).getIdentifier();
        } else if (expression instanceof UQualifiedReferenceExpression) {
            UQualifiedReferenceExpression qualified = (UQualifiedReferenceExpression) expression;
            String receiverIdentifier = getIdentifier(qualified.getReceiver());
            String selectorIdentifier = getIdentifier(qualified.getSelector());
            if (receiverIdentifier == null || selectorIdentifier == null) {
                return null;
            }
            return receiverIdentifier + "." + selectorIdentifier;
        }

        return null;
    }
}
