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

package com.android.tools.lint.checks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.MethodInvocation;

/**
 * Looks for invocations of android.webkit.WebSettings.setJavaScriptEnabled.
 */
public class SetJavaScriptEnabledDetector extends Detector implements Detector.JavaScanner {
    /** Invocations of setJavaScriptEnabled */
    public static final Issue ISSUE = Issue.create("SetJavaScriptEnabled", //$NON-NLS-1$
            "Using `setJavaScriptEnabled`",

            "Your code should not invoke `setJavaScriptEnabled` if you are not sure that " +
            "your app really requires JavaScript support.",

            Category.SECURITY,
            6,
            Severity.WARNING,
            new Implementation(
                    SetJavaScriptEnabledDetector.class,
                    Scope.JAVA_FILE_SCOPE))
            .addMoreInfo(
            "http://developer.android.com/guide/practices/security.html"); //$NON-NLS-1$

    /** Constructs a new {@link SetJavaScriptEnabledDetector} check */
    public SetJavaScriptEnabledDetector() {
    }

    // ---- Implements JavaScanner ----

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        if (node.astArguments().size() == 1
                && !node.astArguments().first().toString().equals("false")) { //$NON-NLS-1$
            context.report(ISSUE, node, context.getLocation(node),
                    "Using `setJavaScriptEnabled` can introduce XSS vulnerabilities " +
                            "into you application, review carefully.");
        }
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("setJavaScriptEnabled");
    }
}
