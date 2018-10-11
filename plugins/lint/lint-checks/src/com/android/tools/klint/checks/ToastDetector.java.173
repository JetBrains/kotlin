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
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.*;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;

/** Detector looking for Toast.makeText() without a corresponding show() call */
public class ToastDetector extends Detector implements Detector.UastScanner {
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

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("makeText"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod uMethod) {
        PsiMethod method = uMethod.getPsi();
        
        if (!JavaEvaluator.isMemberInClass(method, "android.widget.Toast")) {
            return;
        }

        // Make sure you pass the right kind of duration: it's not a delay, it's
        //  LENGTH_SHORT or LENGTH_LONG
        // (see http://code.google.com/p/android/issues/detail?id=3655)
        List<UExpression> args = call.getValueArguments();
        if (args.size() == 3) {
            UExpression duration = args.get(2);
            if (duration instanceof ULiteralExpression) {
                context.report(ISSUE, duration, context.getUastLocation(duration),
                        "Expected duration `Toast.LENGTH_SHORT` or `Toast.LENGTH_LONG`, a custom " +
                                "duration value is not supported");
            }
        }
        
        UElement surroundingDeclaration = UastUtils.getParentOfType(
                call, true,
                UMethod.class, UBlockExpression.class, ULambdaExpression.class);

        if (surroundingDeclaration == null) {
            return;
        }

        ShowFinder finder = new ShowFinder(call);
        surroundingDeclaration.accept(finder);
        if (!finder.isShowCalled()) {
            context.report(ISSUE, call, context.getUastNameLocation(call),
                           "Toast created but not shown: did you forget to call `show()` ?");
        }

    }

    private static class ShowFinder extends AbstractUastVisitor {
        /** The target makeText call */
        private final UCallExpression mTarget;
        /** Whether we've found the show method */
        private boolean mFound;
        /** Whether we've seen the target makeText node yet */
        private boolean mSeenTarget;

        private ShowFinder(UCallExpression target) {
            mTarget = target;
        }

        @Override
        public boolean visitCallExpression(UCallExpression node) {
            if (node.equals(mTarget)) {
                mSeenTarget = true;
            } else {
                if ((mSeenTarget || mTarget.equals(node.getReceiver()))
                    && "show".equals(node.getMethodName())) {
                    // TODO: Do more flow analysis to see whether we're really calling show
                    // on the right type of object?
                    mFound = true;
                }
            }

            return super.visitCallExpression(node);
        }

        @Override
        public boolean visitReturnExpression(UReturnExpression node) {
            if (UastUtils.isChildOf(mTarget, node.getReturnExpression(), true)) {
                // If you just do "return Toast.makeText(...) don't warn
                mFound = true;
            }

            return super.visitReturnExpression(node);
        }

        private boolean isShowCalled() {
            return mFound;
        }
    }
}
