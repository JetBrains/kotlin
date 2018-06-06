/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.ArrayList;
import java.util.List;

/** Detector looking for text messages sent to an unlocalized phone number. */
public class NonInternationalizedSmsDetector extends Detector implements Detector.UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "UnlocalizedSms", //$NON-NLS-1$
            "SMS phone number missing country code",

            "SMS destination numbers must start with a country code or the application code " +
            "must ensure that the SMS is only sent when the user is in the same country as " +
            "the receiver.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            new Implementation(
                    NonInternationalizedSmsDetector.class,
                    Scope.JAVA_FILE_SCOPE));


    /** Constructs a new {@link NonInternationalizedSmsDetector} check */
    public NonInternationalizedSmsDetector() {
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
      List<String> methodNames = new ArrayList<String>(2);
      methodNames.add("sendTextMessage");  //$NON-NLS-1$
      methodNames.add("sendMultipartTextMessage");  //$NON-NLS-1$
      return methodNames;
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod method) {
        if (call.getReceiver() == null) {
            // "sendTextMessage"/"sendMultipartTextMessage" in the code with no operand
            return;
        }

        List<UExpression> args = call.getValueArguments();
        if (args.size() != 5) {
            return;
        }
        UExpression destinationAddress = args.get(0);
        if (!(destinationAddress instanceof ULiteralExpression)) {
            return;
        }
        Object literal = ((ULiteralExpression) destinationAddress).getValue();
        if (!(literal instanceof String)) {
            return;
        }
        String number = (String) literal;
        if (number.startsWith("+")) {  //$NON-NLS-1$
            return;
        }
        context.report(ISSUE, call, context.getUastLocation(destinationAddress),
                "To make sure the SMS can be sent by all users, please start the SMS number " +
                        "with a + and a country code or restrict the code invocation to people in the " +
                        "country you are targeting.");
    }
}