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

import static com.android.SdkConstants.CLASS_FRAGMENT;
import static com.android.SdkConstants.CLASS_V4_FRAGMENT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UAnonymousClass;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;

import java.util.Arrays;
import java.util.List;

/**
 * Checks that Fragment subclasses can be instantiated via
 * {link {@link Class#newInstance()}}: the class is public, static, and has
 * a public null constructor.
 * <p>
 * This helps track down issues like
 *   http://stackoverflow.com/questions/8058809/fragment-activity-crashes-on-screen-rotate
 * (and countless duplicates)
 */
public class FragmentDetector extends Detector implements Detector.UastScanner {
    /** Are fragment subclasses instantiatable? */
    public static final Issue ISSUE = Issue.create(
        "ValidFragment", //$NON-NLS-1$
        "Fragment not instantiatable",

        "From the Fragment documentation:\n" +
        "*Every* fragment must have an empty constructor, so it can be instantiated when " +
        "restoring its activity's state. It is strongly recommended that subclasses do not " +
        "have other constructors with parameters, since these constructors will not be " +
        "called when the fragment is re-instantiated; instead, arguments can be supplied " +
        "by the caller with `setArguments(Bundle)` and later retrieved by the Fragment " +
        "with `getArguments()`.",

        Category.CORRECTNESS,
        6,
        Severity.FATAL,
        new Implementation(
                FragmentDetector.class,
                Scope.JAVA_FILE_SCOPE)
        ).addMoreInfo(
            "http://developer.android.com/reference/android/app/Fragment.html#Fragment()"); //$NON-NLS-1$


    /** Constructs a new {@link FragmentDetector} */
    public FragmentDetector() {
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Arrays.asList(CLASS_FRAGMENT, CLASS_V4_FRAGMENT);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull UClass node) {
        if (node instanceof UAnonymousClass) {
            String message = "Fragments should be static such that they can be re-instantiated by " +
                    "the system, and anonymous classes are not static";
            context.reportUast(ISSUE, node, context.getUastNameLocation(node), message);
            return;
        }

        JavaEvaluator evaluator = context.getEvaluator();
        if (evaluator.isAbstract(node)) {
            return;
        }

        if (!evaluator.isPublic(node)) {
            String message = String.format("This fragment class should be public (%1$s)",
                    node.getQualifiedName());
            context.reportUast(ISSUE, node, context.getUastNameLocation(node), message);
            return;
        }

        if (node.getContainingClass() != null && !evaluator.isStatic(node)) {
            String message = String.format(
                    "This fragment inner class should be static (%1$s)", node.getQualifiedName());
            context.reportUast(ISSUE, node, context.getUastNameLocation(node), message);
            return;
        }

        boolean hasDefaultConstructor = false;
        boolean hasConstructor = false;
        for (PsiMethod constructor : node.getConstructors()) {
            hasConstructor = true;
            if (constructor.getParameterList().getParametersCount() == 0) {
                if (evaluator.isPublic(constructor)) {
                    hasDefaultConstructor = true;
                } else {
                    Location location = context.getNameLocation(constructor);
                    context.report(ISSUE, constructor, location,
                            "The default constructor must be public");
                    // Also mark that we have a constructor so we don't complain again
                    // below since we've already emitted a more specific error related
                    // to the default constructor
                    hasDefaultConstructor = true;
                }
            } else {
                Location location = context.getNameLocation(constructor);
                // TODO: Use separate issue for this which isn't an error
                String message = "Avoid non-default constructors in fragments: "
                        + "use a default constructor plus "
                        + "`Fragment#setArguments(Bundle)` instead";
                context.report(ISSUE, constructor, location, message);
            }
        }

        if (!hasDefaultConstructor && hasConstructor) {
            String message = String.format(
                    "This fragment should provide a default constructor (a public " +
                            "constructor with no arguments) (`%1$s`)",
                    node.getQualifiedName());
            context.reportUast(ISSUE, node, context.getNameLocation(node), message);
        }
    }
}
