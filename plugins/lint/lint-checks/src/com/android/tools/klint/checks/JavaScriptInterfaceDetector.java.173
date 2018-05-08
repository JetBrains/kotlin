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
import com.android.tools.klint.detector.api.TypeEvaluator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Looks for addJavascriptInterface calls on interfaces have been properly annotated
 * with {@code @JavaScriptInterface}
 */
public class JavaScriptInterfaceDetector extends Detector implements Detector.UastScanner {
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

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(ADD_JAVASCRIPT_INTERFACE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod method) {
        if (context.getMainProject().getTargetSdk() < 17) {
            return;
        }

        List<UExpression> arguments = call.getValueArguments();
        if (arguments.size() != 2) {
            return;
        }

        JavaEvaluator evaluator = context.getEvaluator();
        if (!JavaEvaluator.isMemberInClass(method, WEB_VIEW_CLS)) {
            return;
        }

        UExpression first = arguments.get(0);
        PsiType evaluated = TypeEvaluator.evaluate(context, first);
        if (evaluated instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) evaluated;
            PsiClass cls = classType.resolve();
            if (cls == null) {
                return;
            }
            if (isJavaScriptAnnotated(cls)) {
                return;
            }

            Location location = context.getUastNameLocation(call);
            String message = String.format(
                    "None of the methods in the added interface (%1$s) have been annotated " +
                            "with `@android.webkit.JavascriptInterface`; they will not " +
                            "be visible in API 17", cls.getName());
            context.report(ISSUE, call, location, message);
        }
    }

    private static boolean isJavaScriptAnnotated(PsiClass clz) {
        while (clz != null) {
            PsiModifierList modifierList = clz.getModifierList();
            if (modifierList != null
                    && modifierList.findAnnotation(JAVASCRIPT_INTERFACE_CLS) != null) {
                return true;
            }

            for (PsiMethod method : clz.getMethods()) {
                if (method.getModifierList().findAnnotation(JAVASCRIPT_INTERFACE_CLS) != null) {
                    return true;
                }
            }

            clz = clz.getSuperClass();
        }

        return false;
    }
}
