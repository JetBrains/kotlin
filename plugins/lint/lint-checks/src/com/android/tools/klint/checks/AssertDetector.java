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
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.java.JavaSpecialExpressionKinds;
import org.jetbrains.uast.visitor.UastVisitor;

import static org.jetbrains.uast.UastLiteralUtils.isNullLiteral;

/**
 * Looks for assertion usages.
 */
public class AssertDetector extends Detector implements UastScanner {
    /** Using assertions */
    public static final Issue ISSUE = Issue.create(
            "Assert", //$NON-NLS-1$
            "Assertions",

            "Assertions are not checked at runtime. There are ways to request that they be used " +
            "by Dalvik (`adb shell setprop debug.assert 1`), but the property is ignored in " +
            "many places and can not be relied upon. Instead, perform conditional checking " +
            "inside `if (BuildConfig.DEBUG) { }` blocks. That constant is a static final boolean " +
            "which is true in debug builds and false in release builds, and the Java compiler " +
            "completely removes all code inside the if-body from the app.\n" +
            "\n" +
            "For example, you can replace `assert speed > 0` with " +
            "`if (BuildConfig.DEBUG && !(speed > 0)) { throw new AssertionError() }`.\n" +
            "\n" +
            "(Note: This lint check does not flag assertions purely asserting nullness or " +
            "non-nullness; these are typically more intended for tools usage than runtime " +
            "checks.)",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    AssertDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo(
            "https://code.google.com/p/android/issues/detail?id=65183"); //$NON-NLS-1$

    /** Constructs a new {@link com.android.tools.lint.checks.AssertDetector} check */
    public AssertDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    // ---- Implements UastScanner ----

    @Override
    public UastVisitor createUastVisitor(final UastAndroidContext context) {
        return new UastVisitor() {
            @Override
            public boolean visitSpecialExpressionList(@NotNull USpecialExpressionList node) {
                if (node.getKind() != JavaSpecialExpressionKinds.ASSERT) {
                    return true;
                }

                if (!context.getLintContext().getMainProject().isAndroidProject()) {
                    return true;
                }

                UExpression assertion = node.firstOrNull();
                // Allow "assert true"; it's basically a no-op
                if (assertion instanceof ULiteralExpression) {
                    ULiteralExpression literal = (ULiteralExpression) assertion;
                    if (literal.isBoolean()) {
                        Boolean b = ((Boolean)literal.getValue());
                        if (b != null && b) {
                            return false;
                        }
                    }
                } else {
                    // Allow assertions of the form "assert foo != null" because they are often used
                    // to make statements to tools about known nullness properties. For example,
                    // findViewById() may technically return null in some cases, but a developer
                    // may know that it won't be when it's called correctly, so the assertion helps
                    // to clear nullness warnings.
                    if (isNullCheck(assertion)) {
                        return false;
                    }
                }
                String message
                  = "Assertions are unreliable. Use `BuildConfig.DEBUG` conditional checks instead.";
                context.report(ISSUE, node, UastAndroidUtils.getLocation(node), message);
                return false;
            }
        };
    }

    /**
     * Checks whether the given expression is purely a non-null check, e.g. it will return
     * true for expressions like "a != null" and "a != null && b != null" and
     * "b == null || c != null".
     */
    private static boolean isNullCheck(UExpression expression) {
        if (expression instanceof UBinaryExpression) {
            UBinaryExpression binExp = (UBinaryExpression) expression;
            if (isNullLiteral(binExp.getLeftOperand()) ||
                    isNullLiteral(binExp.getRightOperand())) {
                return true;
            } else {
                return isNullCheck(binExp.getLeftOperand()) && isNullCheck(binExp.getRightOperand());
            }
        } else {
            return false;
        }
    }
}
