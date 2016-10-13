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

import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.tools.klint.checks.CleanupDetector.CURSOR_CLS;

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
import com.android.tools.klint.detector.api.TypeEvaluator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;

import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Checks for missing view tag detectors
 */
public class ViewTagDetector extends Detector implements Detector.UastScanner {
    /** Using setTag and leaking memory */
    public static final Issue ISSUE = Issue.create(
            "ViewTag", //$NON-NLS-1$
            "Tagged object leaks",

            "Prior to Android 4.0, the implementation of `View.setTag(int, Object)` would " +
            "store the objects in a static map, where the values were strongly referenced. " +
            "This means that if the object contains any references pointing back to the " +
            "context, the context (which points to pretty much everything else) will leak. " +
            "If you pass a view, the view provides a reference to the context " +
            "that created it. Similarly, view holders typically contain a view, and cursors " +
            "are sometimes also associated with views.",

            Category.PERFORMANCE,
            6,
            Severity.WARNING,
            new Implementation(
                    ViewTagDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link ViewTagDetector} */
    public ViewTagDetector() {
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("setTag");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod method) {
        // The leak behavior is fixed in ICS:
        // http://code.google.com/p/android/issues/detail?id=18273
        if (context.getMainProject().getMinSdk() >= 14) {
            return;
        }

        JavaEvaluator evaluator = context.getEvaluator();
        if (!evaluator.isMemberInSubClassOf(method, CLASS_VIEW, false)) {
            return;
        }
        
        List<UExpression> arguments = call.getValueArguments();
        if (arguments.size() != 2) {
            return;
        }
        UExpression tagArgument = arguments.get(1);
        if (tagArgument == null) {
            return;
        }

        PsiType type = TypeEvaluator.evaluate(context, tagArgument);
        if ((!(type instanceof PsiClassType))) {
            return;
        }
        PsiClass typeClass = ((PsiClassType) type).resolve();
        if (typeClass == null) {
            return;
        }

        String objectType;
        if (InheritanceUtil.isInheritor(typeClass, false, CLASS_VIEW)) {
            objectType = "views";
        } else if (InheritanceUtil.isInheritor(typeClass, false, CURSOR_CLS)) {
            objectType = "cursors";
        } else if (typeClass.getName() != null && typeClass.getName().endsWith("ViewHolder")) {
            objectType = "view holders";
        } else {
            return;
        }
        String message = String.format("Avoid setting %1$s as values for `setTag`: " +
                        "Can lead to memory leaks in versions older than Android 4.0",
                objectType);

        context.report(ISSUE, call, context.getUastLocation(tagArgument), message);
    }
}
