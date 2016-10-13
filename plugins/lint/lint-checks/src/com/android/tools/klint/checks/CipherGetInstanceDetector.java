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
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.ConstantEvaluator;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.collect.Sets;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Ensures that Cipher.getInstance is not called with AES as the parameter.
 */
public class CipherGetInstanceDetector extends Detector implements Detector.UastScanner {
    public static final Issue ISSUE = Issue.create(
            "GetInstance", //$NON-NLS-1$
            "Cipher.getInstance with ECB",
            "`Cipher#getInstance` should not be called with ECB as the cipher mode or without " +
            "setting the cipher mode because the default mode on android is ECB, which " +
            "is insecure.",
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

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(GET_INSTANCE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression node, @NonNull UMethod method) {
        if (!context.getEvaluator().isMemberInSubClassOf(method, CIPHER, false)) {
            return;
        }
        List<UExpression> arguments = node.getValueArguments();
        if (arguments.size() == 1) {
            UExpression expression = arguments.get(0);
            Object value = ConstantEvaluator.evaluate(context, expression);
            if (value instanceof String) {
                checkParameter(context, node, expression, (String)value,
                        !(expression instanceof ULiteralExpression));
            }
        }
    }

    private static void checkParameter(@NonNull JavaContext context,
            @NonNull UCallExpression call, @NonNull UElement node, @NonNull String value,
            boolean includeValue) {
        if (ALGORITHM_ONLY.contains(value)) {
            String message = "`Cipher.getInstance` should not be called without setting the"
                    + " encryption mode and padding";
            context.report(ISSUE, call, context.getUastLocation(node), message);
        } else if (value.contains(ECB)) {
            String message = "ECB encryption mode should not be used";
            if (includeValue) {
                message += " (was \"" + value + "\")";
            }
            context.report(ISSUE, call, context.getUastLocation(node), message);
        }
    }
}
