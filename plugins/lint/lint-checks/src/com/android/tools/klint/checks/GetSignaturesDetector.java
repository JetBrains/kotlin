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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.JavaParser.ResolvedField;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Expression;
import lombok.ast.IntegralLiteral;
import lombok.ast.MethodInvocation;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;

public class GetSignaturesDetector extends Detector implements Detector.JavaScanner  {
    public static final Issue ISSUE = Issue.create(
            "PackageManagerGetSignatures", //$NON-NLS-1$
            "Potential Multiple Certificate Exploit",
            "Improper validation of app signatures could lead to issues where a malicious app " +
                "submits itself to the Play Store with both its real certificate and a fake " +
                "certificate and gains access to functionality or information it shouldn't " +
                "have due to another application only checking for the fake certificate and " +
                "ignoring the rest. Please make sure to validate all signatures returned " +
                "by this method.",
            Category.SECURITY,
            8,
            Severity.INFORMATIONAL,
            new Implementation(
                    GetSignaturesDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo("https://bluebox.com/technical/android-fake-id-vulnerability/");

    private static final String PACKAGE_MANAGER_CLASS = "android.content.pm.PackageManager"; //$NON-NLS-1$
    private static final String GET_PACKAGE_INFO = "getPackageInfo"; //$NON-NLS-1$
    private static final int GET_SIGNATURES_FLAG = 0x00000040; //$NON-NLS-1$

    // ---- Implements JavaScanner ----

    @Override
    @Nullable
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(GET_PACKAGE_INFO);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);

        if (!(resolved instanceof ResolvedMethod) ||
                !((ResolvedMethod) resolved).getContainingClass()
                        .isSubclassOf(PACKAGE_MANAGER_CLASS, false)) {
            return;
        }
        StrictListAccessor<Expression, MethodInvocation> argumentList = node.astArguments();

        // Ignore if the method doesn't fit our description.
        if (argumentList != null && argumentList.size() == 2) {
            TypeDescriptor firstParameterType = context.getType(argumentList.first());
            if (firstParameterType != null
                && firstParameterType.matchesSignature(JavaParser.TYPE_STRING)) {
                maybeReportIssue(calculateValue(context, argumentList.last()), context, node);
            }
        }
    }

    private static void maybeReportIssue(
            int flagValue, JavaContext context, MethodInvocation node) {
        if ((flagValue & GET_SIGNATURES_FLAG) != 0) {
            context.report(ISSUE, node, context.getLocation(node.astArguments().last()),
                "Reading app signatures from getPackageInfo: The app signatures "
                    + "could be exploited if not validated properly; "
                    + "see issue explanation for details.");
        }
    }

    private static int calculateValue(JavaContext context, Expression expression) {
        // This function assumes that the only inputs to the expression are static integer
        // flags that combined via bitwise operands.
        if (expression instanceof IntegralLiteral) {
            return ((IntegralLiteral) expression).astIntValue();
        }

        ResolvedNode resolvedNode = context.resolve(expression);
        if (resolvedNode instanceof ResolvedField) {
            Object value = ((ResolvedField) resolvedNode).getValue();
            if (value instanceof Integer) {
                return (Integer) value;
            }
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            BinaryOperator operator = binaryExpression.astOperator();
            int leftValue = calculateValue(context, binaryExpression.astLeft());
            int rightValue = calculateValue(context, binaryExpression.astRight());

            if (operator == BinaryOperator.BITWISE_OR) {
                return leftValue | rightValue;
            }
            if (operator == BinaryOperator.BITWISE_AND) {
                return leftValue & rightValue;
            }
            if (operator == BinaryOperator.BITWISE_XOR) {
                return leftValue ^ rightValue;
            }
        }

        return 0;
    }

    private static boolean isStringParameter(
            @NonNull Expression expression, @NonNull JavaContext context) {
        if (expression instanceof StringLiteral) {
            return true;
        } else {
            ResolvedNode resolvedNode = context.resolve(expression);
            if (resolvedNode instanceof ResolvedField) {
                if (((ResolvedField) resolvedNode).getValue() instanceof String) {
                    return true;
                }
            }
        }
        return false;
    }
}
