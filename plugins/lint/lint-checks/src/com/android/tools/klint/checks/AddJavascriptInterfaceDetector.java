/*
 * Copyright (C) 2014 The Android Open Source Project
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


import static com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import static com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import static com.android.tools.lint.client.api.JavaParser.TYPE_OBJECT;
import static com.android.tools.lint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.MethodInvocation;

/**
 * Ensures that addJavascriptInterface is not called for API levels below 17.
 */
public class AddJavascriptInterfaceDetector extends Detector implements Detector.JavaScanner {
    public static final Issue ISSUE = Issue.create(
            "AddJavascriptInterface", //$NON-NLS-1$
            "addJavascriptInterface Called",
            "For applications built for API levels below 17, `WebView#addJavascriptInterface` "
                    + "presents a security hazard as JavaScript on the target web page has the "
                    + "ability to use reflection to access the injected object's public fields and "
                    + "thus manipulate the host application in unintended ways.",
            Category.SECURITY,
            9,
            Severity.WARNING,
            new Implementation(
                    AddJavascriptInterfaceDetector.class,
                    Scope.JAVA_FILE_SCOPE)).
            addMoreInfo(
                    "https://labs.mwrinfosecurity.com/blog/2013/09/24/webview-addjavascriptinterface-remote-code-execution/");

    private static final String WEB_VIEW = "android.webkit.WebView"; //$NON-NLS-1$
    private static final String ADD_JAVASCRIPT_INTERFACE = "addJavascriptInterface"; //$NON-NLS-1$

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(ADD_JAVASCRIPT_INTERFACE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        // Ignore the issue if we never build for any API less than 17.
        if (context.getMainProject().getMinSdk() >= 17) {
            return;
        }

        // Ignore if the method doesn't fit our description.
        ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof ResolvedMethod)) {
            return;
        }
        ResolvedMethod method = (ResolvedMethod) resolved;
        if (!method.getContainingClass().isSubclassOf(WEB_VIEW, false)) {
            return;
        }
        if (method.getArgumentCount() != 2
                || !method.getArgumentType(0).matchesName(TYPE_OBJECT)
                || !method.getArgumentType(1).matchesName(TYPE_STRING)) {
            return;
        }

        String message = "`WebView.addJavascriptInterface` should not be called with minSdkVersion < 17 for security reasons: " +
                         "JavaScript can use reflection to manipulate application";
        context.report(ISSUE, node, context.getLocation(node.astName()), message);
    }
}
