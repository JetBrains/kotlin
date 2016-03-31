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
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;

/**
 * Ensures that Cipher.getInstance is not called with AES as the parameter.
 */
public class CipherGetInstanceDetector extends Detector implements Detector.JavaScanner {
    public static final Issue ISSUE = Issue.create(
            "GetInstance", //$NON-NLS-1$
            "Cipher.getInstance with ECB",
            "`Cipher#getInstance` should not be called with ECB as the cipher mode or without "
                    + "setting the cipher mode because the default mode on android is ECB, which "
                    + "is insecure.",
            Category.SECURITY,
            9,
            Severity.WARNING,
            new Implementation(
                    CipherGetInstanceDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    private static final String CIPHER = "javax.crypto.Cipher"; //$NON-NLS-1$
    private static final String GET_INSTANCE = "getInstance"; //$NON-NLS-1$
    private static final Set<String> ALGORITHM_ONLY =
            Sets.newHashSet("AES", "DES", "DESede"); //$NON-NLS-1$
    private static final String ECB = "ECB"; //$NON-NLS-1$

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(GET_INSTANCE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        // Ignore if the method doesn't fit our description.
        JavaParser.ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof JavaParser.ResolvedMethod)) {
            return;
        }
        JavaParser.ResolvedMethod method = (JavaParser.ResolvedMethod) resolved;
        if (!method.getContainingClass().isSubclassOf(CIPHER, false)) {
            return;
        }
        StrictListAccessor<Expression, MethodInvocation> argumentList = node.astArguments();
        if (argumentList != null && argumentList.size() == 1) {
            Expression expression = argumentList.first();
            if (expression instanceof StringLiteral) {
                StringLiteral argument = (StringLiteral)expression;
                String parameter = argument.astValue();
                checkParameter(context, node, argument, parameter, false);
            } else {
                JavaParser.ResolvedNode resolve = context.resolve(expression);
                if (resolve instanceof JavaParser.ResolvedField) {
                    JavaParser.ResolvedField field = (JavaParser.ResolvedField) resolve;
                    Object value = field.getValue();
                    if (value instanceof String) {
                        checkParameter(context, node, expression, (String)value, true);
                    }
                }
            }
        }
    }

    private static void checkParameter(@NonNull JavaContext context,
            @NonNull MethodInvocation call, @NonNull Node node, @NonNull String value,
            boolean includeValue) {
        if (ALGORITHM_ONLY.contains(value)) {
            String message = "`Cipher.getInstance` should not be called without setting the"
                    + " encryption mode and padding";
            context.report(ISSUE, call, context.getLocation(node), message);
        } else if (value.contains(ECB)) {
            String message = "ECB encryption mode should not be used";
            if (includeValue) {
                message += " (was \"" + value + "\")";
            }
            context.report(ISSUE, call, context.getLocation(node), message);
        }
    }
}
