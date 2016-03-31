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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedAnnotation;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.client.api.JavaParser.ResolvedVariable;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.Cast;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.InlineIfExpression;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Looks for addJavascriptInterface calls on interfaces have been properly annotated
 * with {@code @JavaScriptInterface}
 */
public class JavaScriptInterfaceDetector extends Detector implements Detector.JavaScanner {
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

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(ADD_JAVASCRIPT_INTERFACE);
    }

    @Override
    public void visitMethod(
            @NonNull JavaContext context,
            @Nullable AstVisitor visitor,
            @NonNull MethodInvocation call) {
        if (context.getMainProject().getTargetSdk() < 17) {
            return;
        }

        if (call.astArguments().size() != 2) {
            return;
        }

        if (!isCallOnWebView(context, call)) {
            return;
        }

        Expression first = call.astArguments().first();
        ResolvedNode resolved = context.resolve(first);
        if (resolved instanceof ResolvedVariable) {
            // We're passing in a variable to the addJavaScriptInterface method;
            // the variable may be of a more generic type than the actual
            // value assigned to it. For example, we may have a scenario like this:
            //    Object object = new SpecificType();
            //    addJavaScriptInterface(object, ...)
            // Here the type of the variable is Object, but we know that it can
            // contain objects of type SpecificType, so we should check that type instead.
            Node method = JavaContext.findSurroundingMethod(call);
            if (method != null) {
                ConcreteTypeVisitor v = new ConcreteTypeVisitor(context, call);
                method.accept(v);
                resolved = v.getType();
                if (resolved == null) {
                    return;
                }
            } else {
                return;
            }
        } else if (resolved instanceof ResolvedMethod) {
            ResolvedMethod method = (ResolvedMethod) resolved;
            if (method.isConstructor()) {
                resolved = method.getContainingClass();
            } else {
                TypeDescriptor returnType = method.getReturnType();
                if (returnType != null) {
                    resolved = returnType.getTypeClass();
                }
            }
        } else {
            TypeDescriptor type = context.getType(first);
            if (type != null) {
                resolved = type.getTypeClass();
            }
        }

        if (resolved instanceof ResolvedClass) {
            ResolvedClass cls = (ResolvedClass) resolved;
            if (isJavaScriptAnnotated(cls)) {
                return;
            }

            Location location = context.getLocation(call.astName());
            String message = String.format(
                    "None of the methods in the added interface (%1$s) have been annotated " +
                    "with `@android.webkit.JavascriptInterface`; they will not " +
                    "be visible in API 17", cls.getSimpleName());
            context.report(ISSUE, call, location, message);
        }
    }

    private static boolean isCallOnWebView(JavaContext context, MethodInvocation call) {
        ResolvedNode resolved = context.resolve(call);
        if (!(resolved instanceof ResolvedMethod)) {
            return false;
        }
        ResolvedMethod method = (ResolvedMethod) resolved;
        return method.getContainingClass().matches(WEB_VIEW_CLS);

    }

    private static boolean isJavaScriptAnnotated(ResolvedClass clz) {
        while (clz != null) {
            for (ResolvedAnnotation annotation : clz.getAnnotations()) {
                if (annotation.getType().matchesSignature(JAVASCRIPT_INTERFACE_CLS)) {
                    return true;
                }
            }

            for (ResolvedMethod method : clz.getMethods(false)) {
                for (ResolvedAnnotation annotation : method.getAnnotations()) {
                    if (annotation.getType().matchesSignature(JAVASCRIPT_INTERFACE_CLS)) {
                        return true;
                    }
                }
            }

            clz = clz.getSuperClass();
        }

        return false;
    }

    private static class ConcreteTypeVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;
        private final MethodInvocation mTargetCall;
        private boolean mFoundCall;
        private Map<Node, ResolvedClass> mTypes = Maps.newIdentityHashMap();
        private Map<ResolvedVariable, ResolvedClass> mVariableTypes = Maps.newHashMap();

        public ConcreteTypeVisitor(JavaContext context, MethodInvocation call) {
            mContext = context;
            mTargetCall = call;
        }

        public ResolvedClass getType() {
            Expression first = mTargetCall.astArguments().first();
            ResolvedClass resolvedClass = mTypes.get(first);
            if (resolvedClass == null) {
                ResolvedNode resolved = mContext.resolve(first);
                if (resolved instanceof ResolvedVariable) {
                    resolvedClass = mVariableTypes.get(resolved);
                    if (resolvedClass == null) {
                        return ((ResolvedVariable)resolved).getType().getTypeClass();
                    }
                }
            }
            return resolvedClass;
        }

        @Override
        public boolean visitNode(Node node) {
            return mFoundCall || super.visitNode(node);
        }

        @Override
        public void afterVisitMethodInvocation(MethodInvocation node) {
            if (node == mTargetCall) {
                mFoundCall = true;
            }
        }

        @Override
        public void afterVisitConstructorInvocation(@NonNull ConstructorInvocation node) {
            ResolvedNode resolved = mContext.resolve(node);
            if (resolved instanceof ResolvedMethod) {
                ResolvedMethod method = (ResolvedMethod) resolved;
                mTypes.put(node, method.getContainingClass());
            } else {
                // Implicit constructor?
                TypeDescriptor type = mContext.getType(node);
                if (type != null) {
                    ResolvedClass typeClass = type.getTypeClass();
                    if (typeClass != null) {
                        mTypes.put(node, typeClass);
                    }
                }
            }
        }

        @Override
        public void afterVisitVariableReference(VariableReference node) {
            if (mTypes.get(node) == null) {
                ResolvedNode resolved = mContext.resolve(node);
                if (resolved instanceof ResolvedVariable) {
                    ResolvedClass resolvedClass = mVariableTypes.get(resolved);
                    if (resolvedClass != null) {
                        mTypes.put(node, resolvedClass);
                    }
                }
            }
        }

        @Override
        public void afterVisitBinaryExpression(BinaryExpression node) {
            if (node.astOperator() == BinaryOperator.ASSIGN) {
                Expression rhs = node.astRight();
                ResolvedClass resolvedClass = mTypes.get(rhs);
                if (resolvedClass != null) {
                    Expression lhs = node.astLeft();
                    mTypes.put(lhs, resolvedClass);
                    ResolvedNode variable = mContext.resolve(lhs);
                    if (variable instanceof ResolvedVariable) {
                        mVariableTypes.put((ResolvedVariable) variable, resolvedClass);
                    }
                }
            }
        }

        @Override
        public void afterVisitInlineIfExpression(InlineIfExpression node) {
            ResolvedClass resolvedClass = mTypes.get(node.astIfTrue());
            if (resolvedClass == null) {
                resolvedClass = mTypes.get(node.astIfFalse());
            }
            if (resolvedClass != null) {
                mTypes.put(node, resolvedClass);
            }
        }

        @Override
        public void afterVisitVariableDefinitionEntry(VariableDefinitionEntry node) {
            Expression initializer = node.astInitializer();
            if (initializer != null) {
                ResolvedClass resolvedClass = mTypes.get(initializer);
                if (resolvedClass != null) {
                    mTypes.put(node, resolvedClass);
                    ResolvedNode variable = mContext.resolve(node);
                    if (variable instanceof ResolvedVariable) {
                        mVariableTypes.put((ResolvedVariable) variable, resolvedClass);
                    }
                }
            }
        }

        @Override
        public void afterVisitCast(Cast node) {
            ResolvedClass resolvedClass = mTypes.get(node);
            if (resolvedClass != null) {
                mTypes.put(node, resolvedClass);
            }
        }
    }
}
