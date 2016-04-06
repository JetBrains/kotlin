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

package com.android.tools.klint.checks;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;

/**
 * Makes sure that alarms are handled correctly
 */
public class AlarmDetector extends Detector implements UastScanner {

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

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList("setRepeating");
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        UFunction setRepeatingFunction = node.resolve(context);
        UClass containingClass = UastUtils.getContainingClassOrEmpty(setRepeatingFunction);

        if (containingClass.matchesFqName("android.app.AlarmManager")
                && node.getValueArgumentCount() == 4) {
            ensureAtLeast(context, node, 1, 5000L);
            ensureAtLeast(context, node, 2, 60000L);
        }
    }

    private static void ensureAtLeast(@NonNull UastAndroidContext context,
            @NonNull UCallExpression node, int parameter, long min) {
        UExpression arg = node.getValueArguments().get(parameter);
        long value = getLongValue(arg);
        if (value < min) {
            String message = String.format("Value will be forced up to %d as of Android 5.1; "
                    + "don't rely on this to be exact", min);
            context.report(ISSUE, arg, context.getLocation(arg), message);
        }
    }

    private static long getLongValue(@NonNull UExpression argument) {
        Object value = argument.evaluate();
        if (value instanceof Number) {
            return ((Number)value).longValue();
        }

        return Long.MAX_VALUE;
    }
}
