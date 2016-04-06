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
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/** Detector looking for Toast.makeText() without a corresponding show() call */
public class ToastDetector extends Detector implements UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ShowToast", //$NON-NLS-1$
            "Toast created but not shown",

            "`Toast.makeText()` creates a `Toast` but does *not* show it. You must call " +
            "`show()` on the resulting object to actually make the `Toast` appear.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    ToastDetector.class,
                    Scope.JAVA_FILE_SCOPE));


    /** Constructs a new {@link ToastDetector} check */
    public ToastDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }


    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList("makeText"); //$NON-NLS-1$
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        assert "makeText".equals(node.getFunctionName());

        UElement qualifiedExpression = node.getParent();
        if (!(qualifiedExpression instanceof UQualifiedExpression)) {
            return;
        }

        String operand = ((UQualifiedExpression)qualifiedExpression).getReceiver().renderString();
        if (!(operand.equals("Toast") || operand.endsWith(".Toast"))) {
            return;
        }

        // Make sure you pass the right kind of duration: it's not a delay, it's
        //  LENGTH_SHORT or LENGTH_LONG
        // (see http://code.google.com/p/android/issues/detail?id=3655)
        List<UExpression> args = node.getValueArguments();
        if (args.size() == 3) {
            UExpression duration = args.get(2);
            if (duration instanceof ULiteralExpression && ((ULiteralExpression)duration).getValue() instanceof Number) {
                context.report(ISSUE, duration, context.getLocation(duration),
                               "Expected duration `Toast.LENGTH_SHORT` or `Toast.LENGTH_LONG`, a custom " +
                               "duration value is not supported");
            }
        }

        UFunction method = UastUtils.getContainingFunction(node.getParent());
        if (method == null) {
            return;
        }

        UExpression nodeWithPossibleQualifier = UastUtils.getQualifiedCallElement(node);
        ShowFinder finder = new ShowFinder(nodeWithPossibleQualifier);
        finder.process(method);
        if (!finder.isShowCalled()) {
            context.report(ISSUE, node, UastAndroidUtils.getLocation(node),
                           "Toast created but not shown: did you forget to call `show()` ?");
        }
    }

    private static class ShowFinder extends UastVisitor {
        /** The target makeText call */
        private final UExpression mTarget;
        /** Whether we've found the show method */
        private boolean mFound;
        /** Whether we've seen the target makeText node yet */
        private boolean mSeenTarget;

        private ShowFinder(UExpression target) {
            mTarget = target;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            if (node == mTarget) {
                mSeenTarget = true;
            } else if (mSeenTarget && node.functionNameMatches("show")) { //$NON-NLS-1$
                // TODO: Do more flow analysis to see whether we're really calling show
                // on the right type of object?
                mFound = true;
            }

            return true;
        }

        @Override
        public boolean visitQualifiedExpression(@NotNull UQualifiedExpression node) {
            if (node == mTarget) {
                mSeenTarget = true;
            }

            return false;
        }

        @Override
        public boolean visitSpecialExpressionList(@NotNull USpecialExpressionList node) {
            if (node.getKind() == UastSpecialExpressionKind.RETURN && node.firstOrNull() == mTarget) {
                // If you just do "return Toast.makeText(...) don't warn
                mFound = true;
            }

            return false;
        }

        boolean isShowCalled() {
            return mFound && mSeenTarget;
        }
    }
}
