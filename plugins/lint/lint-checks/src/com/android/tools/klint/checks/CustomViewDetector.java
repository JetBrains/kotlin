/*
 * Copyright (C) 2014 The Android Open Source Project
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

import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.CLASS_VIEWGROUP;
import static com.android.SdkConstants.DOT_LAYOUT_PARAMS;
import static com.android.SdkConstants.R_STYLEABLE_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Iterators;

import java.io.File;
import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.Expression;
import lombok.ast.ExpressionStatement;
import lombok.ast.MethodInvocation;
import lombok.ast.StrictListAccessor;

/**
 * Makes sure that custom views use a declare styleable that matches
 * the name of the custom view
 */
public class CustomViewDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            CustomViewDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Mismatched style and class names */
    public static final Issue ISSUE = Issue.create(
            "CustomViewStyleable", //$NON-NLS-1$
            "Mismatched Styleable/Custom View Name",

            "The convention for custom views is to use a `declare-styleable` whose name " +
            "matches the custom view class name. The IDE relies on this convention such that " +
            "for example code completion can be offered for attributes in a custom view " +
            "in layout XML resource files.\n" +
            "\n" +
            "(Similarly, layout parameter classes should use the suffix `_Layout`.)",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    private static final String OBTAIN_STYLED_ATTRIBUTES = "obtainStyledAttributes"; //$NON-NLS-1$

    /** Constructs a new {@link com.android.tools.lint.checks.CustomViewDetector} check */
    public CustomViewDetector() {
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
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(OBTAIN_STYLED_ATTRIBUTES);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        if (node.getParent() instanceof ExpressionStatement) {
            if (!context.isContextMethod(node)) {
                return;
            }
            StrictListAccessor<Expression, MethodInvocation> expressions = node.astArguments();
            int size = expressions.size();
            // Which parameter contains the styleable (attrs) ?
            int parameterIndex;
            if (size == 1) {
                // obtainStyledAttributes(int[] attrs)
                parameterIndex = 0;
            } else {
                // obtainStyledAttributes(int resid, int[] attrs)
                // obtainStyledAttributes(AttributeSet set, int[] attrs)
                // obtainStyledAttributes(AttributeSet set, int[] attrs, int defStyleAttr, int defStyleRes)
                parameterIndex = 1;
            }
            Expression expression = Iterators.get(node.astArguments().iterator(), parameterIndex,
                    null);
            if (expression == null) {
                return;
            }
            String s = expression.toString();
            if (!s.startsWith(R_STYLEABLE_PREFIX)) {
                return;
            }
            String styleableName = s.substring(R_STYLEABLE_PREFIX.length());
            ClassDeclaration cls = JavaContext.findSurroundingClass(node);
            if (cls == null) {
                return;
            }

            ResolvedNode resolved = context.resolve(cls);
            if (!(resolved instanceof ResolvedClass)) {
                return;
            }

            String className = cls.astName().astValue();
            ResolvedClass resolvedClass = (ResolvedClass) resolved;
            if (resolvedClass.isSubclassOf(CLASS_VIEW, false)) {
                if (!styleableName.equals(className)) {
                    String message = String.format(
                        "By convention, the custom view (`%1$s`) and the declare-styleable (`%2$s`) "
                                + "should have the same name (various editor features rely on "
                                + "this convention)",
                        className, styleableName);
                    context.report(ISSUE, node, context.getLocation(expression), message);
                }
            } else if (resolvedClass.isSubclassOf(CLASS_VIEWGROUP + DOT_LAYOUT_PARAMS, false)) {
                ClassDeclaration outer = JavaContext.findSurroundingClass(cls.getParent());
                if (outer == null) {
                    return;
                }
                String layoutClassName = outer.astName().astValue();
                String expectedName = layoutClassName + "_Layout";
                if (!styleableName.equals(expectedName)) {
                    String message = String.format(
                            "By convention, the declare-styleable (`%1$s`) for a layout parameter "
                                    + "class (`%2$s`) is expected to be the surrounding "
                                    + "class (`%3$s`) plus \"`_Layout`\", e.g. `%4$s`. "
                                    + "(Various editor features rely on this convention.)",
                            styleableName, className, layoutClassName, expectedName);
                    context.report(ISSUE, node, context.getLocation(expression), message);
                }
            }
        }
    }
}
