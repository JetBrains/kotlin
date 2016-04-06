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

import com.android.tools.klint.client.api.JavaParser;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.util.Collections;
import java.util.List;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;

public class GetSignaturesDetector extends Detector implements UastScanner {
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

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList(GET_PACKAGE_INFO);
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        UFunction resolved = node.resolve(context);

        if (resolved == null ||
                !UastUtils.getContainingClassOrEmpty(resolved).isSubclassOf(PACKAGE_MANAGER_CLASS)) {
            return;
        }

        List<UExpression> argumentList = node.getValueArguments();

        // Ignore if the method doesn't fit our description.
        if (argumentList.size() == 2) {
            UType firstParameterType = argumentList.get(0).getExpressionType();
            if (firstParameterType != null
                && firstParameterType.matchesFqName(JavaParser.TYPE_STRING)) {
                maybeReportIssue(calculateValue(context, argumentList.get(1)), context, node);
            }
        }
    }

    private static void maybeReportIssue(
            int flagValue, UastAndroidContext context, UCallExpression node) {
        if ((flagValue & GET_SIGNATURES_FLAG) != 0) {
            context.report(ISSUE, node, UastAndroidUtils.getLocation(node.getValueArguments().get(1)),
                "Reading app signatures from getPackageInfo: The app signatures "
                    + "could be exploited if not validated properly; "
                    + "see issue explanation for details.");
        }
    }

    private static int calculateValue(UastAndroidContext context, UExpression expression) {
        // This function assumes that the only inputs to the expression are static integer
        // flags that combined via bitwise operands.
        if (UastLiteralUtils.isIntegralLiteral(expression)) {
            return (int) UastLiteralUtils.getLongValue((ULiteralExpression) expression);
        }

        if (expression instanceof UResolvable) {
            UDeclaration resolvedNode = ((UResolvable) expression).resolve(context);
            if (resolvedNode instanceof UVariable) {
                UExpression initializer = ((UVariable)resolvedNode).getInitializer();
                if (initializer != null) {
                    Object value = initializer.evaluate();
                    if (value instanceof Integer) {
                        return (Integer)value;
                    }
                }
            }
        }


        if (expression instanceof UBinaryExpression) {
            UBinaryExpression binaryExpression = (UBinaryExpression) expression;
            UastBinaryOperator operator = binaryExpression.getOperator();
            int leftValue = calculateValue(context, binaryExpression.getLeftOperand());
            int rightValue = calculateValue(context, binaryExpression.getRightOperand());

            if (operator == UastBinaryOperator.BITWISE_OR) {
                return leftValue | rightValue;
            }
            if (operator == UastBinaryOperator.BITWISE_AND) {
                return leftValue & rightValue;
            }
            if (operator == UastBinaryOperator.BITWISE_XOR) {
                return leftValue ^ rightValue;
            }
        }

        return 0;
    }
}
