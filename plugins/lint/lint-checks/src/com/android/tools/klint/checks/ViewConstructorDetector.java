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

import static com.android.SdkConstants.CLASS_ATTRIBUTE_SET;
import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import static com.android.tools.lint.client.api.JavaParser.ResolvedMethod;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import lombok.ast.ClassDeclaration;
import lombok.ast.Node;
import lombok.ast.NormalTypeBody;

/**
 * Looks for custom views that do not define the view constructors needed by UI builders
 */
public class ViewConstructorDetector extends Detector implements Detector.JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ViewConstructor", //$NON-NLS-1$
            "Missing View constructors for XML inflation",

            "Some layout tools (such as the Android layout editor for Studio & Eclipse) needs to " +
            "find a constructor with one of the following signatures:\n" +
            "* `View(Context context)`\n" +
            "* `View(Context context, AttributeSet attrs)`\n" +
            "* `View(Context context, AttributeSet attrs, int defStyle)`\n" +
            "\n" +
            "If your custom view needs to perform initialization which does not apply when " +
            "used in a layout editor, you can surround the given code with a check to " +
            "see if `View#isInEditMode()` is false, since that method will return `false` " +
            "at runtime but true within a user interface editor.",

            Category.USABILITY,
            3,
            Severity.WARNING,
            new Implementation(
                    ViewConstructorDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Constructs a new {@link ViewConstructorDetector} check */
    public ViewConstructorDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    private static boolean isXmlConstructor(ResolvedMethod method) {
        // Accept
        //   android.content.Context
        //   android.content.Context,android.util.AttributeSet
        //   android.content.Context,android.util.AttributeSet,int
        int argumentCount = method.getArgumentCount();
        if (argumentCount == 0 || argumentCount > 3) {
            return false;
        }
        if (!method.getArgumentType(0).matchesName(CLASS_CONTEXT)) {
            return false;
        }
        if (argumentCount == 1) {
            return true;
        }
        if (!method.getArgumentType(1).matchesName(CLASS_ATTRIBUTE_SET)) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (argumentCount == 2) {
            return true;
        }
        return method.getArgumentType(2).matchesName("int");
    }

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(SdkConstants.CLASS_VIEW);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @Nullable ClassDeclaration node,
            @NonNull Node declarationOrAnonymous, @NonNull ResolvedClass resolvedClass) {
        if (node == null) {
            return;
        }

        // Only applies to concrete classes
        int flags = node.astModifiers().getEffectiveModifierFlags();
        // Ignore abstract classes
        if ((flags & Modifier.ABSTRACT) != 0) {
            return;
        }

        if (node.getParent() instanceof NormalTypeBody
                && ((flags & Modifier.STATIC) == 0)) {
            // Ignore inner classes that aren't static: we can't create these
            // anyway since we'd need the outer instance
            return;
        }

        boolean found = false;
        for (ResolvedMethod constructor : resolvedClass.getConstructors()) {
            if (isXmlConstructor(constructor)) {
                found = true;
                break;
            }
        }

        if (!found) {
            String message = String.format(
                    "Custom view `%1$s` is missing constructor used by tools: "
                            + "`(Context)` or `(Context,AttributeSet)` "
                            + "or `(Context,AttributeSet,int)`",
                    node.astName().astValue());
            Location location = context.getLocation(node.astName());
            context.report(ISSUE, node, location, message  /*data*/);
        }
    }
}
