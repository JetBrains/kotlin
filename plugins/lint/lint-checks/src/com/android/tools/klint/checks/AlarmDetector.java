/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
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

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;

/**
 * Makes sure that alarms are handled correctly
 */
public class AlarmDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            AlarmDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Alarm set too soon/frequently  */
    public static final Issue ISSUE = Issue.create(
            "ShortAlarm", //$NON-NLS-1$
            "Short or Frequent Alarm",

            "Frequent alarms are bad for battery life. As of API 22, the `AlarmManager` " +
            "will override near-future and high-frequency alarm requests, delaying the alarm " +
            "at least 5 seconds into the future and ensuring that the repeat interval is at " +
            "least 60 seconds.\n" +
            "\n" +
            "If you really need to do work sooner than 5 seconds, post a delayed message " +
            "or runnable to a Handler.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Constructs a new {@link AlarmDetector} check */
    public AlarmDetector() {
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
        return Collections.singletonList("setRepeating");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);
        if (resolved instanceof ResolvedMethod) {
            ResolvedMethod method = (ResolvedMethod) resolved;
            if (method.getContainingClass().matches("android.app.AlarmManager")
                    && method.getArgumentCount() == 4) {
                ensureAtLeast(context, node, 1, 5000L);
                ensureAtLeast(context, node, 2, 60000L);
            }
        }
    }

    private static void ensureAtLeast(@NonNull JavaContext context,
            @NonNull MethodInvocation node, int parameter, long min) {
        Iterator<Expression> iterator = node.astArguments().iterator();
        Expression argument = null;
        for (int i = 0; i <= parameter; i++) {
            if (!iterator.hasNext()) {
                return;
            }
            argument = iterator.next();
        }
        if (argument == null) {
            return;
        }

        long value = getLongValue(context, argument);
        if (value < min) {
            String message = String.format("Value will be forced up to %d as of Android 5.1; "
                    + "don't rely on this to be exact", min);
            context.report(ISSUE, argument, context.getLocation(argument), message);
        }
    }

    private static long getLongValue(@NonNull JavaContext context, @NonNull Expression argument) {
        Object value = ConstantEvaluator.evaluate(context, argument);
        if (value instanceof Number) {
            return ((Number)value).longValue();
        }

        return Long.MAX_VALUE;
    }
}
