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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.CLASS_VIEW;
import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;
import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Super;

/**
 * Makes sure that methods call super when overriding methods.
 */
public class CallSuperDetector extends Detector implements Detector.JavaScanner {
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

    // ---- Implements JavaScanner ----

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(MethodDeclaration.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull final JavaContext context) {
        return new ForwardingAstVisitor() {
            @Override
            public boolean visitMethodDeclaration(MethodDeclaration node) {
                ResolvedNode resolved = context.resolve(node);
                if (resolved instanceof ResolvedMethod) {
                    ResolvedMethod method = (ResolvedMethod) resolved;
                    checkCallSuper(context, node, method);
                }

                return false;
            }
        };
    }

    private static void checkCallSuper(@NonNull JavaContext context,
            @NonNull MethodDeclaration declaration,
            @NonNull ResolvedMethod method) {

        ResolvedMethod superMethod = getRequiredSuperMethod(method);
        if (superMethod != null) {
            if (!SuperCallVisitor.callsSuper(context, declaration, superMethod)) {
                String methodName = method.getName();
                String message = "Overriding method should call `super."
                        + methodName + "`";
                Location location = context.getLocation(declaration.astMethodName());
                context.report(ISSUE, declaration, location, message);
            }
        }
    }

    /**
     * Checks whether the given method overrides a method which requires the super method
     * to be invoked, and if so, returns it (otherwise returns null)
     */
    @Nullable
    private static ResolvedMethod getRequiredSuperMethod(
            @NonNull ResolvedMethod method) {

        String name = method.getName();
        if (ON_DETACHED_FROM_WINDOW.equals(name)) {
            // No longer annotated on the framework method since it's
            // now handled via onDetachedFromWindowInternal, but overriding
            // is still dangerous if supporting older versions so flag
            // this for now (should make annotation carry metadata like
            // compileSdkVersion >= N).
            if (!method.getContainingClass().isSubclassOf(CLASS_VIEW, false)) {
                return null;
            }
            return method.getSuperMethod();
        } else if (ON_VISIBILITY_CHANGED.equals(name)) {
            // From Android Wear API; doesn't yet have an annotation
            // but we want to enforce this right away until the AAR
            // is updated to supply it once @CallSuper is available in
            // the support library
            if (!method.getContainingClass().isSubclassOf(
                    "android.support.wearable.watchface.WatchFaceService.Engine", false)) {
                return null;
            }
            return method.getSuperMethod();
        }

        // Look up annotations metadata
        ResolvedMethod directSuper = method.getSuperMethod();
        ResolvedMethod superMethod = directSuper;
        while (superMethod != null) {
            Iterable<JavaParser.ResolvedAnnotation> annotations = superMethod.getAnnotations();
            for (JavaParser.ResolvedAnnotation annotation : annotations) {
                annotation = SupportAnnotationDetector.getRelevantAnnotation(annotation);
                if (annotation != null) {
                    String signature = annotation.getSignature();
                    if (CALL_SUPER_ANNOTATION.equals(signature)) {
                        return directSuper;
                    } else if (signature.endsWith(".OverrideMustInvoke")) {
                        // Handle findbugs annotation on the fly too
                        return directSuper;
                    }
                }
            }
            superMethod = superMethod.getSuperMethod();
        }

        return null;
    }

    /** Visits a method and determines whether the method calls its super method */
    private static class SuperCallVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;
        private final ResolvedMethod mMethod;
        private boolean mCallsSuper;

        public static boolean callsSuper(
                @NonNull JavaContext context,
                @NonNull MethodDeclaration methodDeclaration,
                @NonNull ResolvedMethod method) {
            SuperCallVisitor visitor = new SuperCallVisitor(context, method);
            methodDeclaration.accept(visitor);
            return visitor.mCallsSuper;
        }

        private SuperCallVisitor(@NonNull JavaContext context, @NonNull ResolvedMethod method) {
            mContext = context;
            mMethod = method;
        }

        @Override
        public boolean visitSuper(Super node) {
            ResolvedNode resolved = null;
            if (node.getParent() instanceof MethodInvocation) {
                resolved = mContext.resolve(node.getParent());
            }
            if (resolved == null) {
                resolved = mContext.resolve(node);
            }
            if (mMethod.equals(resolved)) {
                mCallsSuper = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean visitNode(Node node) {
            return mCallsSuper || super.visitNode(node);
        }
    }
}
