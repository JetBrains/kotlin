/*
 * Copyright (C) 2012 The Android Open Source Project
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

import static com.android.SdkConstants.FQCN_SUPPRESS_LINT;
import static com.android.SdkConstants.SUPPRESS_LINT;

import com.android.annotations.NonNull;
import com.android.tools.klint.client.api.IssueRegistry;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.java.JavaUastCallKinds;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Checks annotations to make sure they are valid
 */
public class AnnotationDetector extends Detector implements UastScanner {
    /** Placing SuppressLint on a local variable doesn't work for class-file based checks */
    public static final Issue ISSUE = Issue.create(
            "LocalSuppress", //$NON-NLS-1$
            "@SuppressLint on invalid element",

            "The `@SuppressAnnotation` is used to suppress Lint warnings in Java files. However, " +
            "while many lint checks analyzes the Java source code, where they can find " +
            "annotations on (for example) local variables, some checks are analyzing the " +
            "`.class` files. And in class files, annotations only appear on classes, fields " +
            "and methods. Annotations placed on local variables disappear. If you attempt " +
            "to suppress a lint error for a class-file based lint check, the suppress " +
            "annotation not work. You must move the annotation out to the surrounding method.",

            Category.CORRECTNESS,
            3,
            Severity.ERROR,
            new Implementation(
                    AnnotationDetector.class,
                    Scope.SOURCE_FILE_SCOPE));

    /** Constructs a new {@link AnnotationDetector} check */
    public AnnotationDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements UastScanner ----

    @Override
    public UastVisitor createUastVisitor(UastAndroidContext context) {
        return new AnnotationChecker(context);
    }

    private static class AnnotationChecker extends UastVisitor {
        private final UastAndroidContext mContext;

        public AnnotationChecker(UastAndroidContext context) {
            mContext = context;
        }

        @Override
        public boolean visitAnnotation(@NotNull UAnnotation node) {
            String type = node.getName();
            if (SUPPRESS_LINT.equals(type) || FQCN_SUPPRESS_LINT.equals(type)) {
                UElement parent = node.getParent();
                if (parent instanceof UVariable) {
                    for (UNamedExpression element : node.getValueArguments()) {
                        UExpression valueNode = element.getExpression();
                        if (UastLiteralUtils.isStringLiteral(valueNode)) {
                            ULiteralExpression literal = (ULiteralExpression) valueNode;
                            String id = ((String)literal.getValue());
                            if (!checkId(node, id)) {
                                return super.visitAnnotation(node);
                            }
                        } else if (valueNode instanceof UCallExpression &&
                                   ((UCallExpression) valueNode).getKind() == UastCallKind.ARRAY_INITIALIZER) {
                            UCallExpression array = (UCallExpression) valueNode;
                            if (array.getValueArgumentCount() == 0) {
                                continue;
                            }
                            for (UExpression arrayElement : array.getValueArguments()) {
                                if (UastLiteralUtils.isStringLiteral(arrayElement)) {
                                    String id = ((String)((ULiteralExpression)arrayElement).getValue());
                                    if (!checkId(node, id)) {
                                        return super.visitAnnotation(node);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return super.visitAnnotation(node);
        }

        private boolean checkId(UAnnotation node, String id) {
            IssueRegistry registry = mContext.getLintContext().getDriver().getRegistry();
            Issue issue = registry.getIssue(id);
            // Special-case the ApiDetector issue, since it does both source file analysis
            // only on field references, and class file analysis on the rest, so we allow
            // annotations outside of methods only on fields
            if (issue != null && !issue.getImplementation().getScope().contains(Scope.SOURCE_FILE)
                    || issue == ApiDetector.UNSUPPORTED) {
                // Ensure that this isn't a field
                UElement parent = node.getParent();
                while (parent != null) {
                    if (parent instanceof UFunction || parent instanceof UBlockExpression) {
                        break;
                    } else if (issue == ApiDetector.UNSUPPORTED && parent instanceof UDeclarationsExpression) {
                        UDeclarationsExpression declarations = (UDeclarationsExpression) parent;
                        for (UVariable var : declarations.getVariables()) {
                            if (var.getKind() != UastVariableKind.MEMBER && var.getInitializer() instanceof UQualifiedExpression) {
                                return true;
                            }
                        }
                    }
                    parent = parent.getParent();
                    if (parent == null) {
                        return true;
                    }
                }

                // This issue doesn't have AST access: annotations are not
                // available for local variables or parameters
                mContext.report(ISSUE, node, mContext.getLocation(node), String.format(
                    "The `@SuppressLint` annotation cannot be used on a local " +
                    "variable with the lint check '%1$s': move out to the " +
                    "surrounding method", id));
                return false;
            }

            return true;
        }
    }
}
