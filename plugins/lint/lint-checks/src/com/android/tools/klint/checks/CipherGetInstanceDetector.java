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
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;
import com.google.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;

/**
 * Ensures that Cipher.getInstance is not called with AES as the parameter.
 */
public class CipherGetInstanceDetector extends Detector implements UastScanner {
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
                    Scope.SOURCE_FILE_SCOPE));

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

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList(GET_INSTANCE);
    }

    @Override
    public void visitCall(UastAndroidContext context, UCallExpression node) {
        UClass containingClass = UastUtils.getContainingClass(node);
        if (containingClass == null || !containingClass.isSubclassOf(CIPHER)) {
            return;
        }

        List<UExpression> argumentList = node.getValueArguments();
        if (argumentList.size() == 1) {
            UExpression expression = argumentList.get(0);
            if (expression instanceof ULiteralExpression) {
                ULiteralExpression argument = (ULiteralExpression)expression;
                String parameter = argument.asString();
                checkParameter(context, node, argument, parameter, false);
            } else if (expression instanceof UResolvable) {
                UDeclaration declaration = ((UResolvable)expression).resolve(context);
                if (declaration instanceof UVariable) {
                    UVariable field = (UVariable) declaration;
                    UExpression initializer = field.getInitializer();
                    if (initializer != null) {
                        Object value = initializer.evaluate();
                        if (value instanceof String) {
                            checkParameter(context, node, expression, (String)value, true);
                        }
                    }
                }
            }
        }
    }

    private static void checkParameter(@NonNull UastAndroidContext context,
                                       @NonNull UCallExpression call, @NonNull UExpression arg, @NonNull String value,
                                       boolean includeValue) {
        if (ALGORITHM_ONLY.contains(value)) {
            String message = "`Cipher.getInstance` should not be called without setting the"
                    + " encryption mode and padding";
            context.report(ISSUE, call, context.getLocation(arg), message);
        } else if (value.contains(ECB)) {
            String message = "ECB encryption mode should not be used";
            if (includeValue) {
                message += " (was \"" + value + "\")";
            }
            context.report(ISSUE, call, context.getLocation(arg), message);
        }
    }
}
