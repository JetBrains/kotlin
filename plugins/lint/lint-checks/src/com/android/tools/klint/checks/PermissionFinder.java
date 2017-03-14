/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.klint.checks;

import static com.android.SdkConstants.CLASS_INTENT;
import static com.android.tools.klint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION;
import static com.android.tools.klint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION_READ;
import static com.android.tools.klint.checks.SupportAnnotationDetector.PERMISSION_ANNOTATION_WRITE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.UastLintUtils;
import com.android.tools.klint.detector.api.JavaContext;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiVariable;

import org.jetbrains.uast.*;
import org.jetbrains.uast.util.UastExpressionUtils;

import java.util.List;

/**
 * Utility for locating permissions required by an intent or content resolver
 */
public class PermissionFinder {
    /**
     * Operation that has a permission requirement -- such as a method call,
     * a content resolver read or write operation, an intent, etc.
     */
    public enum Operation {
        CALL, ACTION, READ, WRITE;

        /** Prefix to use when describing a name with a permission requirement */
        public String prefix() {
            switch (this) {
                case ACTION:
                    return "by intent";
                case READ:
                    return "to read";
                case WRITE:
                    return "to write";
                case CALL:
                default:
                    return "by";
            }
        }
    }

    /** A permission requirement given a name and operation */
    public static class Result {
        @NonNull public final PermissionRequirement requirement;
        @NonNull public final String name;
        @NonNull public final Operation operation;

        public Result(
                @NonNull Operation operation,
                @NonNull PermissionRequirement requirement,
                @NonNull String name) {
            this.operation = operation;
            this.requirement = requirement;
            this.name = name;
        }
    }
    
    /**
     * Searches for a permission requirement for the given parameter in the given call
     *
     * @param operation the operation to look up
     * @param context   the context to use for lookup
     * @param parameter the parameter which contains the value which implies the permission
     * @return the result with the permission requirement, or null if nothing is found
     */
    @Nullable
    public static Result findRequiredPermissions(
            @NonNull Operation operation,
            @NonNull JavaContext context,
            @NonNull UElement parameter) {

        // To find the permission required by an intent, we proceed in 3 steps:
        // (1) Locate the parameter in the start call that corresponds to
        //     the Intent
        //
        // (2) Find the place where the intent is initialized, and figure
        //     out the action name being passed to it.
        //
        // (3) Find the place where the action is defined, and look for permission
        //     annotations on that action declaration!

        return new PermissionFinder(context, operation).search(parameter);
    }

    private PermissionFinder(@NonNull JavaContext context, @NonNull Operation operation) {
        mContext = context;
        mOperation = operation;
    }

    @NonNull private final JavaContext mContext;
    @NonNull private final Operation mOperation;
    
    @Nullable
    public Result search(@NonNull UElement node) {
        if (UastLiteralUtils.isNullLiteral(node)) {
            return null;
        } else if (node instanceof UIfExpression) {
            UIfExpression expression = (UIfExpression) node;
            if (expression.getThenExpression() != null) {
                Result result = search(expression.getThenExpression());
                if (result != null) {
                    return result;
                }
            }
            if (expression.getElseExpression() != null) {
                Result result = search(expression.getElseExpression());
                if (result != null) {
                    return result;
                }
            }
        } else if (UastExpressionUtils.isTypeCast(node)) {
            UBinaryExpressionWithType cast = (UBinaryExpressionWithType) node;
            UExpression operand = cast.getOperand();
            return search(operand);
        } else if (node instanceof UParenthesizedExpression) {
            UParenthesizedExpression parens = (UParenthesizedExpression) node;
            UExpression expression = parens.getExpression();
            if (expression != null) {
                return search(expression);
            }
        } else if (UastExpressionUtils.isConstructorCall(node) && mOperation == Operation.ACTION) {
            // Identifies "new Intent(argument)" calls and, if found, continues
            // resolving the argument instead looking for the action definition
            UCallExpression call = (UCallExpression) node;
            UReferenceExpression classReference = call.getClassReference();
            String type = classReference != null ? UastUtils.getQualifiedName(classReference) : null;
            if (CLASS_INTENT.equals(type)) {
                List<UExpression> expressions = call.getValueArguments();
                if (!expressions.isEmpty()) {
                    UExpression action = expressions.get(0);
                    if (action != null) {
                        return search(action);
                    }
                }
            }
            return null;
        } else if (node instanceof UReferenceExpression) {
            PsiElement resolved = ((UReferenceExpression) node).resolve();
            if (resolved instanceof PsiField) {
                UField field = (UField) mContext.getUastContext().convertElementWithParent(resolved, UField.class);
                if (field == null) {
                    return null;
                }
                if (mOperation == Operation.ACTION) {
                    UAnnotation annotation = field.findAnnotation(PERMISSION_ANNOTATION);
                    if (annotation != null) {
                        return getPermissionRequirement(field, annotation);
                    }
                } else if (mOperation == Operation.READ || mOperation == Operation.WRITE) {
                    String fqn = mOperation == Operation.READ
                            ? PERMISSION_ANNOTATION_READ : PERMISSION_ANNOTATION_WRITE;
                    UAnnotation annotation = field.findAnnotation(fqn);
                    if (annotation != null) {
                        List<UNamedExpression> attributes = annotation.getAttributeValues();
                        UNamedExpression o = attributes.size() == 1 ? attributes.get(0) : null;
                        if (o != null && o.getExpression() instanceof UAnnotation) {
                            annotation = (UAnnotation) o.getExpression();
                            if (PERMISSION_ANNOTATION.equals(annotation.getQualifiedName())) {
                                return getPermissionRequirement(field, annotation);
                            }
                        } else {
                            // The complex annotations used for read/write cannot be
                            // expressed in the external annotations format, so they're inlined.
                            // (See Extractor.AnnotationData#write).
                            //
                            // Instead we've inlined the fields of the annotation on the
                            // outer one:
                            return getPermissionRequirement(field, annotation);
                        }
                    }
                } else {
                    assert false : mOperation;
                }
            }
            
            if (resolved instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) resolved;
                UExpression lastAssignment =
                        UastLintUtils.findLastAssignment(variable, node, mContext);

                if (lastAssignment != null) {
                    return search(lastAssignment);
                }
            }
        }

        return null;
    }

    @NonNull
    private Result getPermissionRequirement(
            @NonNull PsiField field,
            @NonNull UAnnotation annotation) {
        PermissionRequirement requirement = PermissionRequirement.create(annotation);
        PsiClass containingClass = field.getContainingClass();
        String name = containingClass != null
                ? containingClass.getName() + "." + field.getName()
                : field.getName();
        assert name != null;
        return new Result(mOperation, requirement, name);
    }
}
