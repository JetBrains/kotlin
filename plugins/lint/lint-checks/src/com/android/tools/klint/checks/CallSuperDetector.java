/*
 * Copyright (C) 2013 The Android Open Source Project
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
import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;

import java.io.File;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Makes sure that methods call super when overriding methods.
 */
public class CallSuperDetector extends Detector implements UastScanner {
    private static final String CALL_SUPER_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "CallSuper"; //$NON-NLS-1$
    private static final String ON_DETACHED_FROM_WINDOW = "onDetachedFromWindow";   //$NON-NLS-1$
    private static final String ON_VISIBILITY_CHANGED = "onVisibilityChanged";      //$NON-NLS-1$

    private static final Implementation IMPLEMENTATION = new Implementation(
            CallSuperDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Missing call to super */
    public static final Issue ISSUE = Issue.create(
            "MissingSuperCall", //$NON-NLS-1$
            "Missing Super Call",

            "Some methods, such as `View#onDetachedFromWindow`, require that you also " +
            "call the super implementation as part of your method.",

            Category.CORRECTNESS,
            9,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Constructs a new {@link CallSuperDetector} check */
    public CallSuperDetector() {
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
    public UastVisitor createUastVisitor(final UastAndroidContext context) {
        return new UastVisitor() {
            @Override
            public boolean visitFunction(@NotNull UFunction node) {
                checkCallSuper(context, node);
                return false;
            }
        };
    }

    private static void checkCallSuper(@NonNull UastAndroidContext context,
            @NonNull UFunction declaration) {

        UFunction superMethod = getRequiredSuperMethod(context, declaration);
        if (superMethod != null) {
            if (!SuperCallVisitor.callsSuper(context, declaration, superMethod)) {
                String methodName = declaration.getName();
                String message = "Overriding method should call `super."
                        + methodName + "`";
                Location location = UastAndroidUtils.getLocation(declaration.getNameElement());
                context.report(ISSUE, declaration, location, message);
            }
        }
    }

    /**
     * Checks whether the given method overrides a method which requires the super method
     * to be invoked, and if so, returns it (otherwise returns null)
     */
    @Nullable
    private static UFunction getRequiredSuperMethod(UastAndroidContext context,
            @NonNull UFunction method) {

        String name = method.getName();
        if (ON_DETACHED_FROM_WINDOW.equals(name)) {
            // No longer annotated on the framework method since it's
            // now handled via onDetachedFromWindowInternal, but overriding
            // is still dangerous if supporting older versions so flag
            // this for now (should make annotation carry metadata like
            // compileSdkVersion >= N).
            if (!UastUtils.getContainingClassOrEmpty(method).isSubclassOf(CLASS_VIEW)) {
                return null;
            }
            List<UFunction> superFunctions = method.getSuperFunctions(context);
            return superFunctions.isEmpty() ? null : superFunctions.get(0);
        } else if (ON_VISIBILITY_CHANGED.equals(name)) {
            // From Android Wear API; doesn't yet have an annotation
            // but we want to enforce this right away until the AAR
            // is updated to supply it once @CallSuper is available in
            // the support library
            if (!UastUtils.getContainingClassOrEmpty(method).isSubclassOf(
                    "android.support.wearable.watchface.WatchFaceService.Engine")) {
                return null;
            }
            List<UFunction> superFunctions = method.getSuperFunctions(context);
            return superFunctions.isEmpty() ? null : superFunctions.get(0);
        }

        // Look up annotations metadata
        List<UFunction> superFunctions = method.getSuperFunctions(context);
        UFunction directSuper = superFunctions.isEmpty() ? null : superFunctions.get(0);
        UFunction superMethod = directSuper;
        while (superMethod != null) {
            for (UAnnotation annotation : superMethod.getAnnotations()) {
                annotation = SupportAnnotationDetector.getRelevantAnnotation(context, annotation);
                if (annotation != null) {
                    String signature = annotation.getFqName();
                    if (CALL_SUPER_ANNOTATION.equals(signature)) {
                        return directSuper;
                    } else if (signature != null && signature.endsWith(".OverrideMustInvoke")) {
                        // Handle findbugs annotation on the fly too
                        return directSuper;
                    }
                }
            }
            superFunctions = superMethod.getSuperFunctions(context);
            superMethod = superFunctions.isEmpty() ? null : superFunctions.get(0);
        }

        return null;
    }

    /** Visits a method and determines whether the method calls its super method */
    private static class SuperCallVisitor extends UastVisitor {
        private final UastAndroidContext mContext;
        private final String mMethodFqName;
        private boolean mCallsSuper;

        public static boolean callsSuper(
                @NonNull UastAndroidContext context,
                @NonNull UFunction methodDeclaration,
                @NonNull UFunction superMethod) {
            SuperCallVisitor visitor = new SuperCallVisitor(context, superMethod);
            visitor.process(methodDeclaration);
            return visitor.mCallsSuper;
        }

        private SuperCallVisitor(@NonNull UastAndroidContext context, @NonNull UFunction method) {
            mContext = context;
            mMethodFqName = UastUtils.getFqName(method);
        }

        @Override
        public boolean visitQualifiedExpression(@NotNull UQualifiedExpression node) {
            UExpression receiver = node.getReceiver();
            UExpression selector = node.getSelector();

            if (receiver instanceof USuperExpression && selector instanceof UCallExpression) {
                UFunction resolvedFunction = ((UCallExpression) selector).resolve(mContext);
                if (mMethodFqName.equals(UastUtils.getFqName(resolvedFunction))) {
                    mCallsSuper = true;
                    return true;
                }
            }

            return false;
        }

        @Override
        public void process(@NotNull UElement element) {
            if (!mCallsSuper) {
                super.process(element);
            }
        }
    }
}
