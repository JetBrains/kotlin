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

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.*;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.uast.*;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;

import static com.android.SdkConstants.*;
import static com.android.tools.klint.detector.api.LintUtils.skipParentheses;

/**
 * Makes sure that custom views use a declare styleable that matches
 * the name of the custom view
 */
public class CustomViewDetector extends Detector implements Detector.UastScanner {

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

    /** Constructs a new {@link CustomViewDetector} check */
    public CustomViewDetector() {
    }

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(OBTAIN_STYLED_ATTRIBUTES);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression node, @NonNull UMethod method) {
        if (skipParentheses(node.getUastParent()) instanceof UExpression) {
            if (!context.getEvaluator().isMemberInSubClassOf(method, CLASS_CONTEXT, false)) {
                return;
            }
            List<UExpression> arguments = node.getValueArguments();
            int size = arguments.size();
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
            UExpression expression = arguments.get(parameterIndex);
            if (!UastUtils.startsWithQualified(expression, R_STYLEABLE_PREFIX)) {
                return;
            }

            List<String> path = UastUtils.asQualifiedPath(expression);
            if (path == null || path.size() < 3) {
                return;
            }

            String styleableName = path.get(2);
            UClass cls = UastUtils.getParentOfType(node, UClass.class, false);
            if (cls == null) {
                return;
            }

            String className = cls.getName();
            if (InheritanceUtil.isInheritor(cls, false, CLASS_VIEW)) {
                if (!styleableName.equals(className)) {
                    String message = String.format(
                            "By convention, the custom view (`%1$s`) and the declare-styleable (`%2$s`) "
                                    + "should have the same name (various editor features rely on "
                                    + "this convention)",
                            className, styleableName);
                    context.report(ISSUE, node, context.getUastLocation(expression), message);
                }
            } else if (InheritanceUtil.isInheritor(cls, false,
                    CLASS_VIEWGROUP + DOT_LAYOUT_PARAMS)) {
                PsiClass outer = PsiTreeUtil.getParentOfType(cls, PsiClass.class, true);
                if (outer == null) {
                    return;
                }
                String layoutClassName = outer.getName();
                String expectedName = layoutClassName + "_Layout";
                if (!styleableName.equals(expectedName)) {
                    String message = String.format(
                            "By convention, the declare-styleable (`%1$s`) for a layout parameter "
                                    + "class (`%2$s`) is expected to be the surrounding "
                                    + "class (`%3$s`) plus \"`_Layout`\", e.g. `%4$s`. "
                                    + "(Various editor features rely on this convention.)",
                            styleableName, className, layoutClassName, expectedName);
                    context.report(ISSUE, node, context.getUastLocation(expression), message);
                }
            }
        }
    }
}
