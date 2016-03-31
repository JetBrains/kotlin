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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.FQCN_SUPPRESS_LINT;
import static com.android.SdkConstants.SUPPRESS_LINT;

import com.android.annotations.NonNull;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.ast.Annotation;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationValue;
import lombok.ast.ArrayInitializer;
import lombok.ast.AstVisitor;
import lombok.ast.Block;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.TypeBody;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;

/**
 * Checks annotations to make sure they are valid
 */
public class AnnotationDetector extends Detector implements Detector.JavaScanner {
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
                    Scope.JAVA_FILE_SCOPE));

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

    // ---- Implements JavaScanner ----

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(Annotation.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new AnnotationChecker(context);
    }

    private static class AnnotationChecker extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public AnnotationChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitAnnotation(Annotation node) {
            String type = node.astAnnotationTypeReference().getTypeName();
            if (SUPPRESS_LINT.equals(type) || FQCN_SUPPRESS_LINT.equals(type)) {
                Node parent = node.getParent();
                if (parent instanceof Modifiers) {
                    parent = parent.getParent();
                    if (parent instanceof VariableDefinition) {
                        for (AnnotationElement element : node.astElements()) {
                            AnnotationValue valueNode = element.astValue();
                            if (valueNode == null) {
                                continue;
                            }
                            if (valueNode instanceof StringLiteral) {
                                StringLiteral literal = (StringLiteral) valueNode;
                                String id = literal.astValue();
                                if (!checkId(node, id)) {
                                    return super.visitAnnotation(node);
                                }
                            } else if (valueNode instanceof ArrayInitializer) {
                                ArrayInitializer array = (ArrayInitializer) valueNode;
                                StrictListAccessor<Expression, ArrayInitializer> expressions =
                                        array.astExpressions();
                                if (expressions == null) {
                                    continue;
                                }
                                Iterator<Expression> arrayIterator = expressions.iterator();
                                while (arrayIterator.hasNext()) {
                                    Expression arrayElement = arrayIterator.next();
                                    if (arrayElement instanceof StringLiteral) {
                                        String id = ((StringLiteral) arrayElement).astValue();
                                        if (!checkId(node, id)) {
                                            return super.visitAnnotation(node);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return super.visitAnnotation(node);
        }

        private boolean checkId(Annotation node, String id) {
            IssueRegistry registry = mContext.getDriver().getRegistry();
            Issue issue = registry.getIssue(id);
            // Special-case the ApiDetector issue, since it does both source file analysis
            // only on field references, and class file analysis on the rest, so we allow
            // annotations outside of methods only on fields
            if (issue != null && !issue.getImplementation().getScope().contains(Scope.JAVA_FILE)
                    || issue == ApiDetector.UNSUPPORTED) {
                // Ensure that this isn't a field
                Node parent = node.getParent();
                while (parent != null) {
                    if (parent instanceof MethodDeclaration
                            || parent instanceof ConstructorDeclaration
                            || parent instanceof Block) {
                        break;
                    } else if (parent instanceof TypeBody) { // It's a field
                        return true;
                    } else if (issue == ApiDetector.UNSUPPORTED
                            && parent instanceof VariableDefinition) {
                        VariableDefinition definition = (VariableDefinition) parent;
                        for (VariableDefinitionEntry entry : definition.astVariables()) {
                            Expression initializer = entry.astInitializer();
                            if (initializer instanceof Select) {
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
