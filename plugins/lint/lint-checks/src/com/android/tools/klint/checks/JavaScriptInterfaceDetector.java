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

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Looks for addJavascriptInterface calls on interfaces have been properly annotated
 * with {@code @JavaScriptInterface}
 */
public class JavaScriptInterfaceDetector extends Detector implements UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "JavascriptInterface", //$NON-NLS-1$
            "Missing @JavascriptInterface on methods",

            "As of API 17, you must annotate methods in objects registered with the " +
            "`addJavascriptInterface` method with a `@JavascriptInterface` annotation.",

            Category.SECURITY,
            8,
            Severity.ERROR,
            new Implementation(
                    JavaScriptInterfaceDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo(
            "http://developer.android.com/reference/android/webkit/WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String)"); //$NON-NLS-1$

    private static final String ADD_JAVASCRIPT_INTERFACE = "addJavascriptInterface"; //$NON-NLS-1$
    private static final String JAVASCRIPT_INTERFACE_CLS = "android.webkit.JavascriptInterface"; //$NON-NLS-1$
    private static final String WEB_VIEW_CLS = "android.webkit.WebView"; //$NON-NLS-1$

    /** Constructs a new {@link JavaScriptInterfaceDetector} check */
    public JavaScriptInterfaceDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.SLOW; // because it relies on class loading referenced javascript interface
    }

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList(ADD_JAVASCRIPT_INTERFACE);
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        if (context.getLintContext().getMainProject().getTargetSdk() < 17) {
            return;
        }

        if (node.getValueArgumentCount() != 2) {
            return;
        }

        if (!isCallOnWebView(context, node)) {
            return;
        }

        UExpression first = node.getValueArguments().get(0);
        UElement resolved = node.resolve(context);
        if (resolved instanceof UVariable) {
            // We're passing in a variable to the addJavaScriptInterface method;
            // the variable may be of a more generic type than the actual
            // value assigned to it. For example, we may have a scenario like this:
            //    Object object = new SpecificType();
            //    addJavaScriptInterface(object, ...)
            // Here the type of the variable is Object, but we know that it can
            // contain objects of type SpecificType, so we should check that type instead.
            UFunction method = UastUtils.getContainingFunction(node);
            if (method != null) {
                ConcreteTypeVisitor v = new ConcreteTypeVisitor(context, node);
                method.accept(v);
                resolved = v.getType();
                if (resolved == null) {
                    return;
                }
            } else {
                return;
            }
        } else if (resolved instanceof UFunction) {
            UFunction method = (UFunction) resolved;
            if (method.getKind() == UastFunctionKind.CONSTRUCTOR) {
                resolved = UastUtils.getContainingClass(method);
            } else {
                UType returnType = method.getReturnType();
                if (returnType != null) {
                    UClass resolvedClass = returnType.resolve(context);
                    if (resolvedClass != null) {
                        resolved = resolvedClass;
                    }
                }
            }
        } else {
            UType type = first.getExpressionType();
            if (type != null) {
                UClass resolvedClass = type.resolve(context);
                if (resolvedClass != null) {
                    resolved = resolvedClass;
                }
            }
        }

        if (resolved instanceof UClass) {
            UClass cls = (UClass) resolved;
            if (isJavaScriptAnnotated(context, cls)) {
                return;
            }

            Location location = UastAndroidUtils.getLocation(node.getFunctionNameElement());
            String message = String.format(
              "None of the methods in the added interface (%1$s) have been annotated " +
              "with `@android.webkit.JavascriptInterface`; they will not " +
              "be visible in API 17", cls.getName());
            context.report(ISSUE, node, location, message);
        }
    }

    private static boolean isCallOnWebView(UastAndroidContext context, UCallExpression call) {
        UFunction resolved = call.resolve(context);
        return UastUtils.getContainingClassOrEmpty(resolved).matchesFqName(WEB_VIEW_CLS);

    }

    private static boolean isJavaScriptAnnotated(UastAndroidContext context, UClass clz) {
        while (clz != null) {
            for (UAnnotation annotation : clz.getAnnotations()) {
                if (JAVASCRIPT_INTERFACE_CLS.equals(annotation.getFqName())) {
                    return true;
                }
            }

            for (UFunction method : clz.getFunctions()) {
                for (UAnnotation annotation : method.getAnnotations()) {
                    if (JAVASCRIPT_INTERFACE_CLS.equals(annotation.getFqName())) {
                        return true;
                    }
                }
            }

            clz = clz.getSuperClass(context);
        }

        return false;
    }

    private static class ConcreteTypeVisitor extends UastVisitor {
        private final UastAndroidContext mContext;
        private final UCallExpression mTargetCall;
        private boolean mFoundCall;
        private Map<UElement, UClass> mTypes = Maps.newIdentityHashMap();
        private Map<UVariable, UClass> mVariableTypes = Maps.newHashMap();

        public ConcreteTypeVisitor(UastAndroidContext context, UCallExpression call) {
            mContext = context;
            mTargetCall = call;
        }

        public UClass getType() {
            UExpression first = mTargetCall.getValueArguments().get(0);
            UClass resolvedClass = mTypes.get(first);
            if (resolvedClass == null && first instanceof UResolvable) {
                UElement resolved = ((UResolvable) first).resolve(mContext);
                if (resolved instanceof UVariable) {
                    resolvedClass = mVariableTypes.get(resolved);
                    if (resolvedClass == null) {
                        return ((UVariable) resolved).getType().resolve(mContext);
                    }
                }
            }
            return resolvedClass;
        }

        @Override
        public boolean visitElement(@NotNull UElement node) {
            return mFoundCall || super.visitElement(node);
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            if (node.getKind() == UastCallKind.FUNCTION_CALL && node == mTargetCall) {
                mFoundCall = true;
            } else if (node.getKind() == UastCallKind.CONSTRUCTOR_CALL) {
                UFunction resolved = node.resolve(mContext);
                if (resolved != null) {
                    mTypes.put(node, UastUtils.getContainingClass(resolved));
                } else {
                    // Implicit constructor?
                    UType type = node.getExpressionType();
                    if (type != null) {
                        UClass typeClass = type.resolve(mContext);
                        if (typeClass != null) {
                            mTypes.put(node, typeClass);
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean visitSimpleReferenceExpression(@NotNull USimpleReferenceExpression node) {
            if (mTypes.get(node) == null) {
                UElement resolved = node.resolve(mContext);
                if (resolved instanceof UVariable) {
                    UClass resolvedClass = mVariableTypes.get(resolved);
                    if (resolvedClass != null) {
                        mTypes.put(node, resolvedClass);
                    }
                }
            }
            return false;
        }

        @Override
        public boolean visitBinaryExpression(@NotNull UBinaryExpression node) {
            if (node.getOperator() == UastBinaryOperator.ASSIGN) {
                UExpression rhs = node.getRightOperand();
                UClass resolvedClass = mTypes.get(rhs);
                if (resolvedClass != null) {
                    UExpression lhs = node.getLeftOperand();
                    mTypes.put(lhs, resolvedClass);
                    if (lhs instanceof UResolvable) {
                        UDeclaration variable = ((UResolvable) lhs).resolve(mContext);
                        if (variable instanceof UVariable) {
                            mVariableTypes.put((UVariable) variable, resolvedClass);
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean visitIfExpression(@NotNull UIfExpression node) {
            if (node.isTernary()) {
                UClass resolvedClass = mTypes.get(node.getThenBranch());
                if (resolvedClass == null) {
                    resolvedClass = mTypes.get(node.getElseBranch());
                }
                if (resolvedClass != null) {
                    mTypes.put(node, resolvedClass);
                }
            }
            return false;
        }

        @Override
        public boolean visitVariable(@NotNull UVariable node) {
            UExpression initializer = node.getInitializer();
            if (initializer != null) {
                UClass resolvedClass = mTypes.get(initializer);
                if (resolvedClass != null) {
                    mTypes.put(node, resolvedClass);
                    mVariableTypes.put(node, resolvedClass);
                }
            }
            return false;
        }

        @Override
        public boolean visitBinaryExpressionWithType(@NotNull UBinaryExpressionWithType node) {
            UClass resolvedClass = mTypes.get(node);
            if (resolvedClass != null) {
                mTypes.put(node, resolvedClass);
            }
            return false;
        }
    }
}
